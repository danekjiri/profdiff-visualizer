package cz.cuni.mff.d3s.profdiffweb.port.profdiff;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.service.ResourceNotFoundException;
import cz.cuni.mff.d3s.profdiffweb.service.RunsFileParsingException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Port interface for interacting with the underlying GraalVM Profdiff library.
 *
 * <p><b>Crucially, this interface acts as an anti-corruption layer that strictly isolates the Profdiff CLI
 * and library dependencies ({@code org.graalvm.profdiff.core.*}) from the rest of the application.</b>
 * Services consuming this port only interact with application-specific Data Transfer Objects (DTOs)
 * and Java primitives, completely shielding the business logic from external library changes,
 * internal parsing mechanics, or CLI-specific concepts.
 */
public interface ProfdiffPort {

    /**
     * Extracts and maps run metadata from the underlying parsed experiment.
     *
     * @param runPath Path to the experiment run directory.
     * @param baseMetadata Previously parsed baseline metadata to combine with the parsed experiment data.
     * @return A {@link RunMetadata} object containing the fully populated run details.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    RunMetadata getRunMetadata(Path runPath, BenchmarkRunMetadata baseMetadata)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Uses the Profdiff library to compute and return a list of the hottest methods in a run.
     *
     * @param runPath Path to the experiment run directory.
     * @param hotPolicy Policy determining the threshold for marking compilation units as hot.
     * @return A list of {@link TopMethod} objects, or an empty list if no profiling data is available.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    List<TopMethod> getTopHotMethods(Path runPath, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Retrieves all compiled methods and their compilation units for a given experiment run.
     *
     * @param runPath Path to the experiment run directory.
     * @param renderingOptions Options configuring the processing of the experiment data.
     * @param hotPolicy Options defining the threshold for marking compilation units as hot.
     * @return A collection of {@link JavaMethod} objects representing the methods.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    Collection<JavaMethod> getCompiledMethods(
            Path runPath, ExperimentProcessingOptions renderingOptions, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Calculates the union of compiled methods from two experiments to prepare them for diffing.
     *
     * @param runPath1 Path to the first experiment run directory.
     * @param runPath2 Path to the second experiment run directory.
     * @param renderingOptions Options configuring the processing of the experiment data.
     * @param hotPolicy Options defining the threshold for marking compilation units as hot.
     * @return A collection of {@link MethodComparisonPair} containing methods found in either or both runs.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    Collection<MethodComparisonPair> getCompiledMethodsUnionPairs(
            Path runPath1, Path runPath2, ExperimentProcessingOptions renderingOptions, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Loads and processes the Inlining Tree for a specific compilation unit via the Profdiff parser.
     *
     * @param runPath Path to the experiment run directory.
     * @param methodName The name of the method.
     * @param compilationId The specific compilation ID to load the tree for.
     * @param renderingOptions Processing options for tree generation.
     * @param hotPolicy Options defining the hotness threshold.
     * @return A {@link TreeResponse} containing the mapped Inlining tree and potential parsing warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    TreeResponse getReportInliningTree(
            Path runPath,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Loads and processes the Optimization Tree for a specific compilation unit via the Profdiff parser.
     *
     * @param runPath Path to the experiment run directory.
     * @param methodName The name of the method.
     * @param compilationId The specific compilation ID to load the tree for.
     * @param renderingOptions Processing options for tree generation.
     * @param hotPolicy Options defining the hotness threshold.
     * @return A {@link TreeResponse} containing the mapped Optimization tree and potential parsing warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    TreeResponse getReportOptimizationTree(
            Path runPath,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Loads and processes the combined Optimization-Context Tree for a specific compilation unit via the Profdiff parser.
     *
     * @param runPath Path to the experiment run directory.
     * @param methodName The name of the method.
     * @param compilationId The specific compilation ID to load the tree for.
     * @param renderingOptions Processing options for tree generation.
     * @param hotPolicy Options defining the hotness threshold.
     * @return A {@link TreeResponse} containing the mapped Optimization-Context tree and potential parsing warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation ID does not exist in the parsed experiment.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    TreeResponse getReportOptimizationContextTree(
            Path runPath,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Executes the tree edit-distance matching algorithm to compute the difference between two Inlining Trees.
     *
     * @param path1 Path to the first experiment run directory.
     * @param path2 Path to the second experiment run directory.
     * @param methodName The name of the method to compare.
     * @param compilationId1 The compilation ID in the first run.
     * @param compilationId2 The compilation ID in the second run.
     * @param renderingOptions Processing options for tree generation.
     * @param hotPolicy Options defining the hotness threshold.
     * @return A {@link TreeResponse} containing the mapped Delta Inlining tree and potential parsing warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation IDs do not exist.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    TreeResponse getComparedInliningTree(
            Path path1,
            Path path2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Executes the tree edit-distance matching algorithm to compute the difference between two Optimization Trees.
     *
     * @param path1 Path to the first experiment run directory.
     * @param path2 Path to the second experiment run directory.
     * @param methodName The name of the method to compare.
     * @param compilationId1 The compilation ID in the first run.
     * @param compilationId2 The compilation ID in the second run.
     * @param renderingOptions Processing options for tree generation.
     * @param hotPolicy Options defining the hotness threshold.
     * @return A {@link TreeResponse} containing the mapped Delta Optimization tree and potential parsing warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation IDs do not exist.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    TreeResponse getComparedOptimizationTree(
            Path path1,
            Path path2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;

    /**
     * Executes the tree edit-distance matching algorithm to compute the difference between two Optimization-Context Trees.
     *
     * @param path1 Path to the first experiment run directory.
     * @param path2 Path to the second experiment run directory.
     * @param methodName The name of the method to compare.
     * @param compilationId1 The compilation ID in the first run.
     * @param compilationId2 The compilation ID in the second run.
     * @param renderingOptions Processing options for tree generation.
     * @param hotPolicy Options defining the hotness threshold.
     * @return A {@link TreeResponse} containing the mapped Delta Optimization-Context tree and potential parsing warnings.
     * @throws RunsFileParsingException if an IO error occur while parsing experiment's logs or profile.
     * @throws ResourceNotFoundException if the method or compilation IDs do not exist.
     * @throws ProfdiffProcessingException if an internal parser error occurs.
     */
    TreeResponse getComparedOptimizationContextTree(
            Path path1,
            Path path2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException;
}
