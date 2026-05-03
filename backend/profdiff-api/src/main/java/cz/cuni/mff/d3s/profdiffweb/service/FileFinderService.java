package cz.cuni.mff.d3s.profdiffweb.service;

import cz.cuni.mff.d3s.profdiffweb.model.dto.WorkspaceDirectory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for finding experiment files in a directory.
 *
 * <p>Provides methods to find experiment files by name, by starting content, and to find the log
 * directory and to list workspace subdirectories.
 */
public interface FileFinderService {
    /**
     * Finds a file by its starting json field in the specified directory.
     *
     * @param directory The directory to search in.
     * @param expectedField The name of the starting field.
     * @param fileExtension The expected file extension (e.g., ".json").
     * @return An Optional containing the Path of the found file, or an empty Optional if no file is
     *     found.
     * @throws IOException If an I/O error occurs while reading the directory or experiment files.
     */
    Optional<Path> findFileByFirstJsonKey(Path directory, String expectedField, String fileExtension)
            throws IOException;

    /**
     * Finds the Optimization log directory within the specified run directory. <a
     * href="https://github.com/oracle/graal/blob/a7a07388f73304b61fe7c83122608cf732a05ef2/compiler/docs/OptimizationLog.md">see
     * Optimization log docs</a>
     *
     * @param runDirectory The run directory to search in.
     * @return An Optional containing the Path of the log directory if found, or an empty Optional if
     *     not found.
     * @throws IOException If an I/O error occurs while reading the directory or experiment files.
     */
    Optional<Path> findLogDirectory(Path runDirectory) throws IOException;

    /**
     * Finds the benchmark results file within the specified run directory.
     * It checks for the standard naming convention first, and falls back to inspecting
     * JSON keys if standard filenames are missing.
     *
     * @param runDirectory The run directory to search in.
     * @return An Optional containing the Path of the benchmark results file if found.
     * @throws IOException If an I/O error occurs while reading the directory or files.
     */
    Optional<Path> findBenchResultsFile(Path runDirectory) throws IOException;

    /**
     * Finds the profiler metadata JSON file within the specified run directory.
     * It scans JSON files to match the expected multi-key structure of a valid profiler file.
     *
     * @param runDirectory The run directory to search in.
     * @return An Optional containing the Path of the profiler file if found.
     * @throws IOException If an I/O error occurs while reading the directory or files.
     */
    Optional<Path> findProfileFile(Path runDirectory) throws IOException;

    /**
     * Reads the given directory and returns a list of absolute paths for its subdirectories with
     * flags if the subdirectory directly contains benchmarks runs.
     *
     * <p>Note: This method catches internally thrown {@link IOException}, {@link SecurityException},
     * and {@link java.nio.file.InvalidPathException} resulting from bad paths. Instead of throwing,
     * it returns an empty list and logs a warning.
     *
     * @param parentPath The absolute or relative parent directory to scan.
     * @return A list of {@link WorkspaceDirectory}, or an empty list if the path is invalid or
     *     without subdirectories.
     */
    List<WorkspaceDirectory> getMarkedSubdirectories(String parentPath);
}
