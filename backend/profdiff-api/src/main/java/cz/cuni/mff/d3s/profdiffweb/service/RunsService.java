package cz.cuni.mff.d3s.profdiffweb.service;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffProcessingException;
import java.util.Collection;
import java.util.List;

/**
 * Service interface for managing runs metadata.
 *
 * <p>Provides method to retrieve metadata for runs from a specified directory.
 */
public interface RunsService {
    /**
     * Retrieves a list of BenchmarkRunMetadata objects from the specified root directory.
     *
     * @param rootPath The String representation of root directory to search for runs. Expecting
     *     following structure:
     *     <pre>
     * rootDir <br>
     * ├── run1 <br>
     * │   ├── scrabble_log <br>
     * │   │   ├── 0_14 <br>
     * │   │   └── ... <br>
     * │   ├── bench-result.json <br>
     * │   └── scrabble_prof.json (mx profjson -E proftool_scrabble_... -o scrabble_prof.json) <br>
     * ├── run2 <br>
     * │   ├── scrabble_log <br>
     * │   │   ├── 0_20 <br>
     * │   │   └── ... <br>
     * │   └── ... <br>
     * ... <br>
     * </pre>
     *
     * @return A list of BenchmarkRunMetadata objects representing the runs found in the directory.
     * @throws ResourceNotFoundException if the path is invalid or cannot be read.
     * @throws IllegalArgumentException if the provided path is null or empty.
     */
    BenchmarkRuns getAllBenchmarksRunsMetadata(String rootPath)
            throws ResourceNotFoundException, IllegalArgumentException;

    /**
     * Retrieves metadata from profiler file and bench-result file for a specific run within the
     * specified directory. If any of the files is missing, the corresponding fields in the returned
     * RunMetadata object will be null.
     *
     * @param path The String representation of the directory containing the runs.
     * @param runName The name of the specific run to retrieve metadata for.
     * @return A RunMetadata object containing detailed information about the specified run.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the run directory cannot be found or metadata could not be
     *     processed.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    RunMetadata getRunMetadata(String path, String runName)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a list of top methods for a specific run within the specified directory. The list is
     * available in {@link org.graalvm.profdiff.core.Experiment} only if the profiler data is present
     * else the list will be empty.
     *
     * @param path The String representation of the directory containing the runs.
     * @param runName The name of the specific run to retrieve top methods for.
     * @param hotPolicy Policy determining how hot methods are calculated.
     * @return A list of TopMethod objects representing the top methods for the specified run.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the specific run directory cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    List<TopMethod> getTopHotMethods(String path, String runName, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a collection of {@link JavaMethod} objects for a specific run within the specified
     * directory. All the parsed {@link org.graalvm.profdiff.core.Method} are available in {@link
     * org.graalvm.profdiff.core.Experiment} and transformed into {@link JavaMethod} objects.
     *
     * @param path The String representation of the directory containing the runs.
     * @param runName The name of the specific run to retrieve Java methods for.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return A collection of JavaMethod objects representing the Java methods for the specified run.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the specific run directory cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    Collection<JavaMethod> getCompiledMethods(
            String path, String runName, ExperimentProcessingOptions renderingOptions, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a collection of JavaMethods representing the intersection of methods found in both
     * runs.
     *
     * @param path Absolute path to runs directory.
     * @param runName1 Name of first run.
     * @param runName2 Name of second run.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return Collection of intersected JavaMethod objects.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if any of the specific run directories cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    Collection<MethodComparisonPair> getCompiledMethodsUnionPairs(
            String path,
            String runName1,
            String runName2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a {@link TreeResponse} object representing the Inlining tree for a specific method
     * within a specific run.
     *
     * @param path The path to the directory containing the runs.
     * @param runName The name of the specific run.
     * @param methodName The name of the method.
     * @param compilationId The ID of the specific compilation unit.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return TreeResponse object containing the rendered Inlining tree and any warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the specific run directory cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    TreeResponse getReportInliningTree(
            String path,
            String runName,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a {@link TreeResponse} object representing the Optimization tree for a specific
     * method within a specific run.
     *
     * @param path The path to the directory containing the runs.
     * @param runName The name of the specific run.
     * @param methodName The name of the method.
     * @param compilationId The ID of the specific compilation unit.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return TreeResponse object containing the rendered Optimization tree and any warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the specific run directory cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    TreeResponse getReportOptimizationTree(
            String path,
            String runName,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a {@link TreeResponse} object representing the Optimization-Context tree for a
     * specific method within a specific run.
     *
     * @param path The path to the directory containing the runs.
     * @param runName The name of the specific run.
     * @param methodName The name of the method.
     * @param compilationId The ID of the specific compilation unit.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return TreeResponse object containing the rendered Optimization-Context tree and any warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the specific run directory cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    TreeResponse getReportOptimizationContextTree(
            String path,
            String runName,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a {@link TreeResponse} object representing the compared (diff) Inlining tree between
     * two runs.
     *
     * @param path The String representation of the directory containing the runs.
     * @param runName1 The name of the first run to compare.
     * @param runName2 The name of the second run to compare.
     * @param methodName The name of the method to retrieve the diffing tree for.
     * @param compilationId1 The ID of the specific compilation unit in the first run.
     * @param compilationId2 The ID of the specific compilation unit in the second run.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return TreeResponse object containing the compared Inlining tree and any warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if any of the specified run directories cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    TreeResponse getComparedInliningTree(
            String path,
            String runName1,
            String runName2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a {@link TreeResponse} object representing the compared (diff) Optimization tree
     * between two runs.
     *
     * @param path The String representation of the directory containing the runs.
     * @param runName1 The name of the first run to compare.
     * @param runName2 The name of the second run to compare.
     * @param methodName The name of the method to retrieve the diffing tree for.
     * @param compilationId1 The ID of the specific compilation unit in the first run.
     * @param compilationId2 The ID of the specific compilation unit in the second run.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return TreeResponse object containing the compared Optimization tree and any warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if any of the specified run directories cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    TreeResponse getComparedOptimizationTree(
            String path,
            String runName1,
            String runName2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;

    /**
     * Retrieves a {@link TreeResponse} object representing the compared (diff) Optimization-Context
     * tree between two runs.
     *
     * @param path The String representation of the directory containing the runs.
     * @param runName1 The name of the first run to compare.
     * @param runName2 The name of the second run to compare.
     * @param methodName The name of the method to retrieve the diffing tree for.
     * @param compilationId1 The ID of the specific compilation unit in the first run.
     * @param compilationId2 The ID of the specific compilation unit in the second run.
     * @param renderingOptions Processing options.
     * @param hotPolicy Hotness options.
     * @return TreeResponse object containing the compared Optimization-Context tree and any warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if any of the specified run directories cannot be found.
     * @throws IllegalArgumentException if the provided path is null or empty.
     * @throws ProfdiffProcessingException if an error occurs during library processing.
     */
    TreeResponse getComparedOptimizationContextTree(
            String path,
            String runName1,
            String runName2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException,
                    IllegalArgumentException;
}
