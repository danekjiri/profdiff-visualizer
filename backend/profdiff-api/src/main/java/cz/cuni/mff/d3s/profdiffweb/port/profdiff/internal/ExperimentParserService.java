package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffProcessingException;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache.ParsedExperimentCache;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import cz.cuni.mff.d3s.profdiffweb.service.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.graalvm.profdiff.core.Experiment;
import org.graalvm.profdiff.core.ExperimentId;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.Writer;
import org.graalvm.profdiff.parser.ExperimentFiles;
import org.graalvm.profdiff.parser.ExperimentFilesImpl;
import org.graalvm.profdiff.parser.ExperimentParser;
import org.graalvm.profdiff.parser.ExperimentParserError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that handles parsing and caching raw Profdiff's experiment.
 *
 * <p>Provides methods to retrieve and cache {@link Experiment} objects parsed from given
 * run directories. It uses a {@link FileFinderService} to locate necessary files and an {@link
 * ParsedExperimentCache} to store previously parsed experiments for efficiency.
 */
@Singleton
public class ExperimentParserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentParserService.class);

    private final FileFinderService fileFinder;
    private final MetadataParserService metadataParser;
    private final ParsedExperimentCache parsedExperimentCache;

    @Inject
    public ExperimentParserService(
            FileFinderService fileFinder,
            MetadataParserService metadataParser,
            ParsedExperimentCache parsedExperimentCache) {
        this.fileFinder = fileFinder;
        this.metadataParser = metadataParser;
        this.parsedExperimentCache = parsedExperimentCache;
    }

    /**
     * Retrieves an {@link Experiment} for the specified run directory using default {@link
     * OptionValues}.
     *
     * <p>This is a convenience wrapper for cases where specific tree rendering or diffing options are
     * not required (e.g. when retrieving general report metadata or the list of top methods). It
     * delegates to {@link #getParsedExperimentResult(Path, OptionValues)} with a default,
     * unconfigured set of options.
     *
     * @param runPath The path to the run directory containing profiling data.
     * @return The parsed Experiment object with default settings.
     * @see #getParsedExperimentResult(Path, OptionValues)
     */
    public ExperimentResult getParsedExperimentResult(Path runPath)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        return getParsedExperimentResult(runPath, new OptionValues());
    }

    /**
     * Retrieves an {@link Experiment} for the specified run directory using specific processing
     * options with the use of caching for performance.
     *
     * <p>This method is typically called when preparing data for tree visualization or diffing, where
     * {@code options} (like pruning identities or sorting trees) are critical.
     *
     * @param runPath The path to the run directory containing profiling data.
     * @param options The {@link OptionValues} to use during parsing (e.g., for filtering trees).
     */
    public ExperimentResult getParsedExperimentResult(Path runPath, OptionValues options)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        var cachedExperiment = parsedExperimentCache.get(runPath.toString());
        return cachedExperiment.orElseGet(() -> {
            var experimentResult = parseExperiment(runPath, options);
            parsedExperimentCache.put(runPath.toString(), experimentResult);
            return experimentResult;
        });
    }

    /**
     * Attempts to extract the CompilationKind (JIT or AOT) from the profiler file.
     *
     * @param profileFile The path to the profile file.
     * @return The parsed {@link Experiment.CompilationKind}, or null if the file is missing or
     *     corrupt.
     */
    private Experiment.CompilationKind getCompilationKindFromFile(Optional<Path> profileFile) {
        if (profileFile.isEmpty()) {
            return null;
        }

        try {
            var profilerMetadata = metadataParser.parseProfilerMetadata(profileFile.get());
            return parseStringCompilationKind(profilerMetadata.compilationKind());
        } catch (RunsFileParsingException e) {
            LOGGER.debug(
                    "Failed to extract compilation kind from profiler file for {}: {}", profileFile, e.getMessage());
        }
        return null;
    }

    private static Experiment.CompilationKind parseStringCompilationKind(String compilationKind) {
        if (compilationKind == null) {
            return null;
        }

        return switch (compilationKind) {
            case "JIT" -> Experiment.CompilationKind.JIT;
            case "AOT" -> Experiment.CompilationKind.AOT;
            default -> {
                LOGGER.warn(
                        "Unrecognized CompilationKind encountered in profiler file: '{}'. Defaulting to null.",
                        compilationKind);
                yield null;
            }
        };
    }

    /**
     * Parses an {@link Experiment} from the specified run directory path. It locates the necessary
     * optimization log directory and optional profiler JSON (result of Linux perf command), then uses
     * the profdiff library to parse the experiment data.
     *
     * @param runPath The path to the run directory containing GRAAL logs.
     * @param options The rendering and processing options.
     */
    private ExperimentResult parseExperiment(Path runPath, OptionValues options)
            throws ResourceNotFoundException, RunsFileParsingException, ProfdiffProcessingException {
        String profileFilePath, optimizationLogPath;
        Experiment.CompilationKind compilationKind = null;
        try {
            var profileFileOpt = fileFinder.findProfileFile(runPath);
            compilationKind = getCompilationKindFromFile(profileFileOpt);
            profileFilePath = profileFileOpt.map(Path::toString).orElse(null);
            optimizationLogPath = fileFinder
                    .findLogDirectory(runPath)
                    .map(Path::toString)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Optimization log directory not found in run directory: " + runPath));
        } catch (IOException e) {
            throw new RunsFileParsingException("Failed to access required files in directory: " + runPath, e);
        }

        var warningWriter = Writer.stringBuilder(options);
        ExperimentFiles files =
                new ExperimentFilesImpl(ExperimentId.AUXILIARY, compilationKind, profileFilePath, optimizationLogPath);
        ExperimentParser parser = new ExperimentParser(files, warningWriter);
        List<String> warnings = warningWriter.getOutput().isBlank()
                ? Collections.emptyList()
                : Arrays.asList(warningWriter.getOutput().split("\\R"));
        try {
            var experiment = parser.parse();
            return new ExperimentResult(experiment, warnings);
        } catch (IOException e) {
            throw new RunsFileParsingException(
                    "An IO Error occurs while parsing experiment from path '" + runPath + "'", e);
        } catch (ExperimentParserError e) {
            throw new RunsFileParsingException(e.getMessage(), e);
        }
    }
}
