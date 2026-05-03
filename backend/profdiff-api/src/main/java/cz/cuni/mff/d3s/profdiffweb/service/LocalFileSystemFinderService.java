package cz.cuni.mff.d3s.profdiffweb.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import cz.cuni.mff.d3s.profdiffweb.model.dto.BenchResultsMetadata;
import cz.cuni.mff.d3s.profdiffweb.model.dto.ProfileMetadata;
import cz.cuni.mff.d3s.profdiffweb.model.dto.WorkspaceDirectory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service implementation for finding files in a directory. */
@Singleton
public class LocalFileSystemFinderService implements FileFinderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileSystemFinderService.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final List<String> allowedPathPrefixes;

    @Inject
    public LocalFileSystemFinderService(
            @Value("${app.security.allowed-path-prefixes:}") List<String> allowedPathPrefixes) {
        this.allowedPathPrefixes = allowedPathPrefixes;
    }

    @Override
    public Optional<Path> findFileByFirstJsonKey(Path directory, String expectedField, String fileExtension)
            throws IOException {
        try (DirectoryStream<Path> directoryFiles = Files.newDirectoryStream(directory)) {
            for (Path entry : directoryFiles) {
                if (!Files.isRegularFile(entry) || !entry.toString().endsWith(fileExtension)) {
                    continue;
                }

                if (hasFirstJsonField(entry, expectedField)) {
                    return Optional.of(entry);
                } else {
                    LOGGER.debug(
                            "Skipping '{}': Content does not start with expected JSON field '{}'.",
                            entry.getFileName(),
                            expectedField);
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<Path> findLogDirectory(Path runDirectory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(runDirectory)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && isOptimizationLogDirectory(entry)) {
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Path> findBenchResultsFile(Path runDirectory) throws IOException {
        /// check for common file name first
        Path newFormat = runDirectory.resolve(BenchResultsMetadata.EXPECTED_FILE_NAME);
        if (Files.isRegularFile(newFormat)) {
            return Optional.of(newFormat);
        }

        /// as fallback try to find by JSON key name
        LOGGER.debug(
                "Standard benchmark results filenames not found in '{}', trying to find by JSON key.",
                runDirectory.getFileName());
        return findFileByFirstJsonKey(runDirectory, BenchResultsMetadata.EXPECTED_FIRST_JSON_KEY, ".json");
    }

    @Override
    public Optional<Path> findProfileFile(Path runDirectory) throws IOException {
        try (DirectoryStream<Path> directoryFiles = Files.newDirectoryStream(runDirectory)) {
            for (Path entry : directoryFiles) {
                if (!Files.isRegularFile(entry) || !entry.toString().endsWith(".json")) {
                    continue;
                }

                if (isProfilerFile(entry)) {
                    return Optional.of(entry);
                } else {
                    LOGGER.debug(
                            "Skipping '{}': Content does not start with expected profiler JSON fields.",
                            entry.getFileName());
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public List<WorkspaceDirectory> getMarkedSubdirectories(String parentPath) {
        if (!isPathAllowed(parentPath)) {
            List<WorkspaceDirectory> synthetic = buildSyntheticRoots(parentPath);
            if (!synthetic.isEmpty()) {
                return synthetic;
            }
            LOGGER.warn("Rejected directory request for disallowed path: '{}'", parentPath);
            return List.of();
        }

        try (Stream<Path> stream = Files.list(Paths.get(parentPath))) {
            return stream.filter(Files::isDirectory)
                    .map(p -> new WorkspaceDirectory(
                            p.toAbsolutePath().normalize().toString(), checkForRuns(p)))
                    .toList();
        } catch (IOException | SecurityException | InvalidPathException e) {
            LOGGER.warn("Failed to retrieve subdirectories for path '{}': {}", parentPath, e.getMessage());
            return List.of();
        }
    }

    /**
     * Semantically checks if the first property in a JSON object matches the expected field name.
     * Safely ignores BOMs, whitespaces, and newlines.
     */
    private static boolean hasFirstJsonField(Path file, String expectedField) {
        try (JsonParser parser = JSON_FACTORY.createParser(file.toFile())) {
            if (parser.nextToken() == JsonToken.START_OBJECT) {
                if (parser.nextToken() == JsonToken.FIELD_NAME) {
                    return expectedField.equals(parser.currentName());
                }
            }
        } catch (IOException e) {
            LOGGER.error(
                    "File '{}' is not a valid JSON or could not be parsed: {}", file.getFileName(), e.getMessage());
        }
        return false;
    }

    /**
     * Checks if the first two keys in the JSON file are exactly 'compilationKind' and 'totalPeriod',
     * and ensures the 'code' field appears as the 3rd or 4th key and is an array.
     * Fails fast if a different key structure is encountered.
     */
    private static boolean isProfilerFile(Path file) {
        try (JsonParser parser = JSON_FACTORY.createParser(file.toFile())) {
            if (parser.nextToken() == JsonToken.START_OBJECT) {
                /// first compilationKind json field
                if (parser.nextToken() != JsonToken.FIELD_NAME
                        || !parser.currentName().equals(ProfileMetadata.EXPECTED_FIRST_JSON_KEY)) {
                    return false;
                }
                parser.nextToken();
                parser.skipChildren();

                /// second totalPeriod json field
                if (parser.nextToken() != JsonToken.FIELD_NAME
                        || !parser.currentName().equals(ProfileMetadata.EXPECTED_SECOND_JSON_KEY)) {
                    return false;
                }
                parser.nextToken();
                parser.skipChildren();

                /// third or forth code json field
                if (parser.nextToken() != JsonToken.FIELD_NAME) {
                    return false;
                }
                if (ProfileMetadata.EXPECTED_THIRD_OR_FORTH_JSON_KEY.equals(parser.currentName())) {
                    return parser.nextToken() == JsonToken.START_ARRAY;
                }
                parser.nextToken();
                parser.skipChildren();
                if (parser.nextToken() != JsonToken.FIELD_NAME) {
                    return false;
                }
                if (ProfileMetadata.EXPECTED_THIRD_OR_FORTH_JSON_KEY.equals(parser.currentName())) {
                    /// check if code is array of profiling
                    return parser.nextToken() == JsonToken.START_ARRAY;
                }
            }
        } catch (IOException e) {
            LOGGER.error(
                    "File '{}' is not a valid JSON or could not be parsed: {}", file.getFileName(), e.getMessage());
        }
        return false;
    }

    /** Checks if given path is root of benchmarks runs using {@link #findLogDirectory(Path)}. */
    private boolean checkForRuns(Path path) {
        try (var subdirectories = Files.newDirectoryStream(path)) {
            for (var entry : subdirectories) {
                if (Files.isDirectory(entry) && findLogDirectory(entry).isPresent()) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not check if '{}' contains runs: {}", path, e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a directory contains optimization logs files.
     *
     * @throws IOException if listing files or reading file content fails.
     */
    private static boolean isOptimizationLogDirectory(Path subdirectory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(subdirectory)) {
            boolean hasFiles = false;

            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    hasFiles = true;
                    if (hasFirstJsonField(entry, "methodName")) {
                        return true;
                    }
                }
            }

            if (!hasFiles) {
                LOGGER.info("Directory '{}' rejected: No regular files found.", subdirectory.getFileName());
            }
            return false;
        }
    }

    /**
     * Returns true when no prefix restrictions are configured, or when the
     * normalised absolute path starts with at least one of the allowed prefixes.
     */
    private boolean isPathAllowed(String parentPath) {
        if (allowedPathPrefixes.isEmpty()) {
            return true;
        }
        try {
            String normalized = Paths.get(parentPath).toAbsolutePath().normalize().toString();
            return allowedPathPrefixes.stream().anyMatch(normalized::startsWith);
        } catch (InvalidPathException e) {
            LOGGER.warn("Could not normalize path '{}': {}", parentPath, e.getMessage());
            return false;
        }
    }

    /**
     * When a path is disallowed but is an ancestor of one or more allowed prefixes,
     * returns those prefixes as synthetic {@link WorkspaceDirectory} entries.
     * This lets the frontend hint {@code /workspace} and {@code /default-benchmarks}
     * when the user types {@code /} in the prod Docker environment.
     */
    private List<WorkspaceDirectory> buildSyntheticRoots(String parentPath) {
        if (allowedPathPrefixes.isEmpty()) {
            return List.of();
        }
        try {
            Path normalized = Paths.get(parentPath).toAbsolutePath().normalize();
            return allowedPathPrefixes.stream()
                    .map(Paths::get)
                    .filter(allowed -> allowed.startsWith(normalized))
                    .filter(Files::isDirectory)
                    .map(allowed -> new WorkspaceDirectory(
                            allowed.toAbsolutePath().normalize().toString(),
                            checkForRuns(allowed)))
                    .toList();
        } catch (InvalidPathException e) {
            LOGGER.warn("Could not normalize path '{}' during synthetic root build: {}", parentPath, e.getMessage());
            return List.of();
        }
    }
}
