package cz.cuni.mff.d3s.profdiffweb.service;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffPort;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffProcessingException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RunsServiceImpl implements RunsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunsServiceImpl.class);

    private final FileFinderService fileFinder;
    private final MetadataParserService metadataParser;
    private final ProfdiffPort profdiffPort;

    @Inject
    public RunsServiceImpl(
            FileFinderService fileFinder, MetadataParserService metadataParser, ProfdiffPort profdiffPort) {
        this.fileFinder = fileFinder;
        this.metadataParser = metadataParser;
        this.profdiffPort = profdiffPort;
    }

    private record MetadataExtractionResult(BenchmarkRunMetadata metadata, WarningMessage fatalError) {
        boolean isSuccess() {
            return metadata != null;
        }

        static MetadataExtractionResult success(BenchmarkRunMetadata data) {
            return new MetadataExtractionResult(data, null);
        }

        static MetadataExtractionResult failure(WarningMessage error) {
            return new MetadataExtractionResult(null, error);
        }
    }

    @Override
    public BenchmarkRuns getAllBenchmarksRunsMetadata(String rootPath)
            throws ResourceNotFoundException, IllegalArgumentException {
        var rootDirectory = getAndValidateRunPath(rootPath, "");

        try (Stream<Path> runDirectories = Files.list(rootDirectory).filter(Files::isDirectory)) {
            var results = runDirectories.map(this::extractRunMetadata).toList();

            var validRuns = results.stream()
                    .filter(MetadataExtractionResult::isSuccess)
                    .map(MetadataExtractionResult::metadata)
                    .toList();
            var generalWarnings = results.stream()
                    .filter(r -> !r.isSuccess())
                    .map(MetadataExtractionResult::fatalError)
                    .toList();
            return new BenchmarkRuns(validRuns, generalWarnings);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Error reading root directory: " + rootPath, e);
        }
    }

    @Override
    public RunMetadata getRunMetadata(String path, String runName)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        Path runPath = getAndValidateRunPath(path, runName);
        var runMetadata = extractRunMetadata(runPath);
        if (!runMetadata.isSuccess()) {
            String error = runMetadata.fatalError().message();
            throw new ResourceNotFoundException(
                    "Run metadata could not be processed for run '" + runName + "': " + error);
        }

        return profdiffPort.getRunMetadata(runPath, runMetadata.metadata());
    }

    @Override
    public List<TopMethod> getTopHotMethods(String path, String runName, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        Path runPath = getAndValidateRunPath(path, runName);
        return profdiffPort.getTopHotMethods(runPath, hotPolicy);
    }

    @Override
    public Collection<JavaMethod> getCompiledMethods(
            String path, String runName, ExperimentProcessingOptions renderingOptions, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        Path runPath = getAndValidateRunPath(path, runName);
        return profdiffPort.getCompiledMethods(runPath, renderingOptions, hotPolicy);
    }

    @Override
    public Collection<MethodComparisonPair> getCompiledMethodsUnionPairs(
            String path,
            String runName1,
            String runName2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        Path runPath1 = getAndValidateRunPath(path, runName1);
        Path runPath2 = getAndValidateRunPath(path, runName2);

        return profdiffPort.getCompiledMethodsUnionPairs(runPath1, runPath2, renderingOptions, hotPolicy);
    }

    @Override
    public TreeResponse getReportInliningTree(
            String path,
            String runName,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        var runPath = getAndValidateRunPath(path, runName);

        return profdiffPort.getReportInliningTree(runPath, methodName, compilationId, renderingOptions, hotPolicy);
    }

    @Override
    public TreeResponse getReportOptimizationTree(
            String path,
            String runName,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        var runPath = getAndValidateRunPath(path, runName);

        return profdiffPort.getReportOptimizationTree(runPath, methodName, compilationId, renderingOptions, hotPolicy);
    }

    @Override
    public TreeResponse getReportOptimizationContextTree(
            String path,
            String runName,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        var runPath = getAndValidateRunPath(path, runName);

        return profdiffPort.getReportOptimizationContextTree(
                runPath, methodName, compilationId, renderingOptions, hotPolicy);
    }

    @Override
    public TreeResponse getComparedInliningTree(
            String path,
            String runName1,
            String runName2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        if (compilationId2 == null || compilationId2.isBlank()) {
            return getReportInliningTree(path, runName1, methodName, compilationId1, renderingOptions, hotPolicy);
        }
        if (compilationId1 == null || compilationId1.isBlank()) {
            return getReportInliningTree(path, runName2, methodName, compilationId2, renderingOptions, hotPolicy);
        }

        Path path1 = getAndValidateRunPath(path, runName1), path2 = getAndValidateRunPath(path, runName2);
        return profdiffPort.getComparedInliningTree(
                path1, path2, methodName, compilationId1, compilationId2, renderingOptions, hotPolicy);
    }

    @Override
    public TreeResponse getComparedOptimizationTree(
            String path,
            String runName1,
            String runName2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        if (compilationId2 == null || compilationId2.isBlank()) {
            return getReportOptimizationTree(path, runName1, methodName, compilationId1, renderingOptions, hotPolicy);
        }
        if (compilationId1 == null || compilationId1.isBlank()) {
            return getReportOptimizationTree(path, runName2, methodName, compilationId2, renderingOptions, hotPolicy);
        }

        Path path1 = getAndValidateRunPath(path, runName1), path2 = getAndValidateRunPath(path, runName2);
        return profdiffPort.getComparedOptimizationTree(
                path1, path2, methodName, compilationId1, compilationId2, renderingOptions, hotPolicy);
    }

    @Override
    public TreeResponse getComparedOptimizationContextTree(
            String path,
            String runName1,
            String runName2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException {
        if (compilationId2 == null || compilationId2.isBlank()) {
            return getReportOptimizationContextTree(
                    path, runName1, methodName, compilationId1, renderingOptions, hotPolicy);
        }
        if (compilationId1 == null || compilationId1.isBlank()) {
            return getReportOptimizationContextTree(
                    path, runName2, methodName, compilationId2, renderingOptions, hotPolicy);
        }

        Path path1 = getAndValidateRunPath(path, runName1), path2 = getAndValidateRunPath(path, runName2);
        return profdiffPort.getComparedOptimizationContextTree(
                path1, path2, methodName, compilationId1, compilationId2, renderingOptions, hotPolicy);
    }

    /**
     * Make the path absolute and safely concatenates it with the runName. Also check if path exists
     * and is valid.
     *
     * @param path Absolute or relative path to benchmarks root.
     * @param runName Chosen benchmarks run name.
     * @return Absolute path consisting the root folder and chosen benchmarks run name.
     * @throws ResourceNotFoundException If the path or resulting run directory is invalid/does not
     *     exist.
     * @throws IllegalArgumentException If the provided path is null or empty.
     */
    private static Path getAndValidateRunPath(String path, String runName)
            throws ResourceNotFoundException, IllegalArgumentException {
        if (path == null || path.isBlank()) {
            LOGGER.warn("Run validation failed: Client provided null or empty root path.");
            throw new IllegalArgumentException("Provided path is null or empty.");
        }

        Path rootPath = Path.of(path).toAbsolutePath().normalize();
        if (!Files.isDirectory(rootPath)) {
            throw new ResourceNotFoundException("Provided path is not a valid directory: '" + path + "'");
        }

        if (runName == null || runName.isBlank()) {
            return rootPath;
        }

        Path runPath = rootPath.resolve(runName);
        if (!Files.isDirectory(runPath)) {
            throw new ResourceNotFoundException("Run directory '" + runName + "' does not exist.");
        }

        return runPath;
    }

    /**
     * Processes a single run directory to extract metadata after validating the presence of required
     * files.
     *
     * <p>If the required files are not found or cannot be parsed, it logs a warning and returns an
     * empty Optional that will be filtered out in the list of runs. If bench-results.json not found,
     * the metadata will not be included in the result. If profiler JSON file is not found, then
     * diffing without profiling data will be performed. The optimization log is a mandatory file, and
     * if it is not found, the run will be skipped.
     *
     * @param runDir The path to the run directory to process.
     * @return An Optional containing the BenchmarkRunMetadata if processing is successful, or empty
     *     if any required file is missing or cannot be parsed.
     */
    private MetadataExtractionResult extractRunMetadata(Path runDir) {
        List<WarningMessage> runWarnings = new ArrayList<>();

        try {
            var logDirectory = fileFinder.findLogDirectory(runDir);
            if (logDirectory.isEmpty()) {
                var msg = "[RUN '" + runDir.getFileName() + "'] Skipped, no optimization logs found.";
                LOGGER.warn(msg);
                return MetadataExtractionResult.failure(WarningMessage.of(msg, "LOG_MISSING"));
            }
        } catch (IOException e) {
            var msg = "[RUN '" + runDir.getFileName() + "'] Skipped, IO error while accessing run directory.";
            LOGGER.error(msg, e);
            return MetadataExtractionResult.failure(WarningMessage.of(msg, "IO_ERROR"));
        }

        ProfileMetadata profilerData = null;
        try {
            var profilerJson = fileFinder.findProfileFile(runDir);
            if (profilerJson.isPresent()) {
                try {
                    profilerData = metadataParser.parseProfilerMetadata(profilerJson.get());
                } catch (RunsFileParsingException e) {
                    String message =
                            "[RUN '" + runDir.getFileName() + "'] Profiler JSON found but corrupt: " + e.getMessage();
                    LOGGER.warn(message);
                    runWarnings.add(WarningMessage.of(message, "PROFILER_ERROR"));
                }
            } else {
                String msg = "[RUN '" + runDir.getFileName() + "'] Profiler data is missing or invalid JSON keys.";
                LOGGER.warn(msg);
                runWarnings.add(WarningMessage.of(msg, "PROFILER_MISSING"));
            }
        } catch (IOException e) {
            String msg = "[RUN '" + runDir.getFileName() + "'] IO Error looking for profiler data: " + e.getMessage();
            LOGGER.warn(msg);
            runWarnings.add(WarningMessage.of(msg, "IO_ERROR"));
        }

        BenchResultsMetadata benchResultsData = null;
        try {
            Optional<Path> benchResultsJson = fileFinder.findBenchResultsFile(runDir);
            if (benchResultsJson.isPresent()) {
                try {
                    var parsedBenchResults = metadataParser.parseBenchResults(benchResultsJson.get());
                    benchResultsData = parsedBenchResults.metadata();

                    if (!parsedBenchResults.warnings().isEmpty()) {
                        runWarnings.addAll(parsedBenchResults.warnings());
                    }
                } catch (RunsFileParsingException e) {
                    String msg =
                            "[RUN '" + runDir.getFileName() + "'] Benchmark results JSON is corrupt: " + e.getMessage();
                    LOGGER.warn(msg);
                    runWarnings.add(WarningMessage.of(msg, "METADATA_ERROR"));
                }
            } else {
                String msg = "[RUN '" + runDir.getFileName()
                        + "'] Benchmark results JSON is missing (could not find bench-results.json, results.json, or file starting with 'queries').";
                LOGGER.warn(msg);
                runWarnings.add(WarningMessage.of(msg, "METADATA_MISSING"));
            }
        } catch (IOException e) {
            String msg = "[RUN '" + runDir.getFileName() + "'] IO Error looking for benchmark results data: "
                    + e.getMessage();
            LOGGER.warn(msg);
            runWarnings.add(WarningMessage.of(msg, "IO_ERROR"));
        }

        return MetadataExtractionResult.success(
                new BenchmarkRunMetadata(runDir.getFileName().toString(), benchResultsData, profilerData, runWarnings));
    }
}
