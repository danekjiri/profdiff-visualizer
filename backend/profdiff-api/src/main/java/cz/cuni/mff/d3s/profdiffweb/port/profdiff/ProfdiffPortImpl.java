package cz.cuni.mff.d3s.profdiffweb.port.profdiff;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache.PreparedExperimentCache;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import cz.cuni.mff.d3s.profdiffweb.service.ResourceNotFoundException;
import cz.cuni.mff.d3s.profdiffweb.service.RunsFileParsingException;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.graalvm.profdiff.core.*;
import org.graalvm.profdiff.core.inlining.InliningTree;
import org.graalvm.profdiff.core.optimization.OptimizationTree;
import org.graalvm.profdiff.parser.ExperimentParserError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProfdiffPortImpl implements ProfdiffPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfdiffPortImpl.class);

    private final ExperimentParserService experimentParserService;
    private final PreparedExperimentCache preparedExperimentCache;
    private final ExecutorService fragmentExecutor;
    private final ApiExperimentMatcher matcher = new ApiExperimentMatcher();

    private record ExperimentPairResult(ExperimentResult result1, ExperimentResult result2) {}

    @Inject
    public ProfdiffPortImpl(
            ExperimentParserService experimentParserService, PreparedExperimentCache preparedExperimentCache) {
        this.experimentParserService = experimentParserService;
        this.preparedExperimentCache = preparedExperimentCache;
        this.fragmentExecutor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /** Prevents thread leaks when the Micronaut context shuts down. */
    @PreDestroy
    public void shutdownExecutor() {
        fragmentExecutor.shutdown();
        try {
            if (!fragmentExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                fragmentExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fragmentExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public RunMetadata getRunMetadata(Path runPath, BenchmarkRunMetadata baseMetadata)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        ExperimentResult result = experimentParserService.getParsedExperimentResult(runPath);
        return ProfdiffMapper.mapRunMetadata(baseMetadata, result.experiment());
    }

    @Override
    public List<TopMethod> getTopHotMethods(Path runPath, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        var experiment =
                experimentParserService.getParsedExperimentResult(runPath).experiment();
        if (!experiment.isProfileAvailable()) {
            LOGGER.info("No profiling data available for run '{}'.", runPath);
            return List.of();
        }

        ProfdiffMapper.toHotPolicy(hotPolicy).markHotCompilationUnits(experiment);
        return experiment
                .getTopMethods(10)
                .map(method -> ProfdiffMapper.mapTopMethod(method, experiment.getTotalPeriod()))
                .toList();
    }

    @Override
    public Collection<JavaMethod> getCompiledMethods(
            Path runPath, ExperimentProcessingOptions renderingOptions, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues graalOptions =
                ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var preparedExperimentResult = getPreparedSingleExperiment(runPath, graalOptions);

        List<JavaMethod> methods = new ArrayList<>();
        for (Method m : preparedExperimentResult.experiment().getMethodsByName().getValues()) {
            methods.add(ProfdiffMapper.mapMethod(m));
        }
        return methods;
    }

    @Override
    public Collection<MethodComparisonPair> getCompiledMethodsUnionPairs(
            Path runPath1, Path runPath2, ExperimentProcessingOptions renderingOptions, HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        ExperimentPairResult pair = getPreparedCompareExperimentPair(runPath1, runPath2, options);

        return getMethodsUnion(pair.result1(), pair.result2());
    }

    @Override
    public TreeResponse getReportInliningTree(
            Path runPath,
            String methodName,
            String compId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var experimentResult = getPreparedSingleExperiment(runPath, options);

        InliningTree tree;
        var compilationUnit = getMethodsCompilationUnitById(experimentResult.experiment(), methodName, compId);
        try {
            tree = compilationUnit.loadInliningTree();
        } catch (ExperimentParserError e) {
            throw new ProfdiffProcessingException(e.getMessage());
        }
        tree.preprocess(options);

        var renderedTree = TreeRenderer.convert(tree.getRoot(), options);
        List<WarningMessage> warnings = experimentResult.warnings().stream()
                .map(w -> WarningMessage.of(w, "PARSER_WARNING"))
                .toList();

        return new TreeResponse(
                new RenderedTreeNode(
                        RenderedTreeNode.Marker.INFO,
                        ProfdiffMapper.createRawContent("Inlining tree"),
                        List.of(renderedTree)),
                warnings);
    }

    @Override
    public TreeResponse getReportOptimizationTree(
            Path runPath,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var experimentResult = getPreparedSingleExperiment(runPath, options);

        var compilationUnit = getMethodsCompilationUnitById(experimentResult.experiment(), methodName, compilationId);
        OptimizationTree tree;
        try {
            tree = compilationUnit.loadOptimizationTree();
        } catch (ExperimentParserError e) {
            throw new ProfdiffProcessingException(e.getMessage());
        }
        tree.preprocess(options);

        var renderedTree = TreeRenderer.convert(tree.getRoot(), options);
        var warnings = experimentResult.warnings().stream()
                .map(w -> WarningMessage.of(w, "PARSER_WARNING"))
                .toList();

        return new TreeResponse(
                new RenderedTreeNode(
                        RenderedTreeNode.Marker.INFO,
                        ProfdiffMapper.createRawContent("Optimization tree"),
                        List.of(renderedTree)),
                warnings);
    }

    @Override
    public TreeResponse getReportOptimizationContextTree(
            Path runPath,
            String methodName,
            String compilationId,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var experimentResult = getPreparedSingleExperiment(runPath, options);

        var compilationUnit = getMethodsCompilationUnitById(experimentResult.experiment(), methodName, compilationId);
        CompilationUnit.TreePair treePair;
        try {
            treePair = compilationUnit.loadTrees();
        } catch (ExperimentParserError e) {
            throw new ProfdiffProcessingException(e.getMessage());
        }
        var optimizationContextTree =
                OptimizationContextTree.createFrom(treePair.getInliningTree(), treePair.getOptimizationTree());

        var renderedTree = TreeRenderer.convert(optimizationContextTree.getRoot(), options);
        var warnings = experimentResult.warnings().stream()
                .map(w -> WarningMessage.of(w, "PARSER_WARNING"))
                .toList();
        ///  already prefixed with tree name
        return new TreeResponse(renderedTree, warnings);
    }

    @Override
    public TreeResponse getComparedInliningTree(
            Path path1,
            Path path2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var pair = getPreparedCompareExperimentPair(path1, path2, options);
        var cu1 = getMethodsCompilationUnitById(pair.result1().experiment(), methodName, compilationId1);
        var cu2 = getMethodsCompilationUnitById(pair.result2().experiment(), methodName, compilationId2);

        InliningTree tree1, tree2;
        try {
            tree1 = cu1.loadInliningTree();
            tree2 = cu2.loadInliningTree();
        } catch (ExperimentParserError e) {
            throw new ProfdiffProcessingException(e.getMessage());
        }
        tree1.preprocess(options);
        tree2.preprocess(options);

        var renderedTree = matcher.getInliningTreeDiff(tree1, tree2, options);
        return new TreeResponse(
                new RenderedTreeNode(
                        RenderedTreeNode.Marker.INFO,
                        ProfdiffMapper.createRawContent("Inlining tree"),
                        List.of(renderedTree)),
                getComparisonWarnings(options, path1, path2));
    }

    @Override
    public TreeResponse getComparedOptimizationTree(
            Path path1,
            Path path2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var pair = getPreparedCompareExperimentPair(path1, path2, options);
        var cu1 = getMethodsCompilationUnitById(pair.result1().experiment(), methodName, compilationId1);
        var cu2 = getMethodsCompilationUnitById(pair.result2().experiment(), methodName, compilationId2);

        OptimizationTree tree1, tree2;
        try {
            tree1 = cu1.loadOptimizationTree();
            tree2 = cu2.loadOptimizationTree();
        } catch (ExperimentParserError e) {
            throw new ProfdiffProcessingException(e.getMessage());
        }
        tree1.preprocess(options);
        tree2.preprocess(options);

        var renderedTree = matcher.getOptimizationTreeDiff(tree1, tree2, options);
        return new TreeResponse(
                new RenderedTreeNode(
                        RenderedTreeNode.Marker.INFO,
                        ProfdiffMapper.createRawContent("Optimization tree"),
                        List.of(renderedTree)),
                getComparisonWarnings(options, path1, path2));
    }

    @Override
    public TreeResponse getComparedOptimizationContextTree(
            Path path1,
            Path path2,
            String methodName,
            String compilationId1,
            String compilationId2,
            ExperimentProcessingOptions renderingOptions,
            HotPolicyOptions hotPolicy)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        OptionValues options = ProfdiffMapper.toOptionValues(renderingOptions, ProfdiffMapper.toHotPolicy(hotPolicy));
        var pair = getPreparedCompareExperimentPair(path1, path2, options);
        var cu1 = getMethodsCompilationUnitById(pair.result1().experiment(), methodName, compilationId1);
        var cu2 = getMethodsCompilationUnitById(pair.result2().experiment(), methodName, compilationId2);

        CompilationUnit.TreePair treePair1, treePair2;
        try {
            treePair1 = cu1.loadTrees();
            treePair2 = cu2.loadTrees();
        } catch (ExperimentParserError e) {
            throw new ProfdiffProcessingException(e.getMessage());
        }
        treePair1.getInliningTree().preprocess(options);
        treePair1.getOptimizationTree().preprocess(options);
        treePair2.getInliningTree().preprocess(options);
        treePair2.getOptimizationTree().preprocess(options);

        var renderedTree = matcher.getOptimizationContextTreeDiff(treePair1, treePair2, options);
        ///  already prefixed with tree name
        return new TreeResponse(renderedTree, getComparisonWarnings(options, path1, path2));
    }

    private CompilationUnit getMethodsCompilationUnitById(Experiment exp, String methodName, String compId) {
        Method m = exp.getMethodsByName().get(methodName);
        if (m == null) {
            throw new ResourceNotFoundException("Method '" + methodName + "' not found.");
        }

        return m.getCompilationUnits().stream()
                .filter(u -> u.getCompilationId().equals(compId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Compilation ID '" + compId + "' not found."));
    }

    /**
     * Retrieves or prepares an experiment for a single run scenario.
     *
     * <p>This method checks the cache for an existing prepared experiment using the provided path and
     * options. If not found, it:
     *
     * <ol>
     *   <li>Parses the experiment raw data.
     *   <li>Marks hot compilation units if profile data is available.
     *   <li>Creates compilation fragments for methods hot in this run.
     *   <li>Caches and returns the result.
     * </ol>
     *
     * @param runPath Path to the experiment run directory.
     * @param options Options defining hotness policy and processing flags.
     * @return The prepared experiment result.
     */
    private ExperimentResult getPreparedSingleExperiment(Path runPath, OptionValues options)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        var key = PreparedExperimentCache.Key.forSeparate(runPath.toString(), options);

        return preparedExperimentCache.get(key).orElseGet(() -> {
            var result = experimentParserService.getParsedExperimentResult(runPath, options);
            if (result.experiment().isProfileAvailable()) {
                options.getHotCompilationUnitPolicy().markHotCompilationUnits(result.experiment());
            }
            var selfHotMethods = result.experiment().getHotMethodsNames();

            boolean fragmentsCreated = createFragmentsParallel(result.experiment(), options, selfHotMethods);
            if (fragmentsCreated) {
                preparedExperimentCache.put(key, result);
                return result;
            } else {
                List<String> warnings = new ArrayList<>(result.warnings());
                warnings.add("Failed to extract compilation fragments.");
                return new ExperimentResult(result.experiment(), warnings);
            }
        });
    }

    /**
     * Retrieves or prepares an experiment pair for a comparison scenario.
     *
     * <p>This method differs from {@link #getPreparedSingleExperiment} by processing both the
     * requested run and its comparison partner simultaneously. It calculates the union of hot methods
     * from both experiments. This ensures that compilation fragments are created for methods that are
     * hot in <em>either</em> run, allowing for consistent diffing even if a method is cold in one of
     * the runs.
     *
     * @param run1Path Path to the first experiment run directory.
     * @param run2Path Path to the second experiment run directory.
     * @param options Options defining hotness policy and processing flags.
     */
    private ExperimentPairResult getPreparedCompareExperimentPair(Path run1Path, Path run2Path, OptionValues options)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        var key1 = PreparedExperimentCache.Key.forComparison(run1Path.toString(), run2Path.toString(), options);
        var key2 = PreparedExperimentCache.Key.forComparison(run2Path.toString(), run1Path.toString(), options);
        Optional<ExperimentResult> cached1 = preparedExperimentCache.get(key1);
        Optional<ExperimentResult> cached2 = preparedExperimentCache.get(key2);

        if (cached1.isPresent() && cached2.isPresent()) {
            cached1.get().experiment().setExperimentId(ExperimentId.ONE);
            cached2.get().experiment().setExperimentId(ExperimentId.TWO);
            return new ExperimentPairResult(cached1.get(), cached2.get());
        }

        var experimentRes1 =
                cached1.orElseGet(() -> experimentParserService.getParsedExperimentResult(run1Path, options));
        var experimentRes2 =
                cached2.orElseGet(() -> experimentParserService.getParsedExperimentResult(run2Path, options));
        if (cached1.isEmpty() && experimentRes1.experiment().isProfileAvailable()) {
            options.getHotCompilationUnitPolicy().markHotCompilationUnits(experimentRes1.experiment());
        }
        if (cached2.isEmpty() && experimentRes2.experiment().isProfileAvailable()) {
            options.getHotCompilationUnitPolicy().markHotCompilationUnits(experimentRes2.experiment());
        }

        var union = new HashSet<>(experimentRes1.experiment().getHotMethodsNames());
        union.addAll(experimentRes2.experiment().getHotMethodsNames());

        if (cached1.isEmpty()) {
            boolean frag1Created = createFragmentsParallel(experimentRes1.experiment(), options, union);
            if (frag1Created) {
                preparedExperimentCache.put(key1, experimentRes1);
            } else {
                experimentRes1 =
                        appendWarning(experimentRes1, "Failed to generate compilation fragments for " + run1Path + ".");
            }
        }

        if (cached2.isEmpty()) {
            boolean frag2Created = createFragmentsParallel(experimentRes2.experiment(), options, union);
            if (frag2Created) {
                preparedExperimentCache.put(key2, experimentRes2);
            } else {
                experimentRes2 =
                        appendWarning(experimentRes2, "Failed to generate compilation fragments for " + run2Path + ".");
            }
        }

        experimentRes1.experiment().setExperimentId(ExperimentId.ONE);
        experimentRes2.experiment().setExperimentId(ExperimentId.TWO);
        return new ExperimentPairResult(experimentRes1, experimentRes2);
    }

    private ExperimentResult appendWarning(ExperimentResult result, String warning) {
        List<String> warnings = new ArrayList<>(result.warnings());
        warnings.add(warning);
        return new ExperimentResult(result.experiment(), warnings);
    }

    /**
     * Generates compilation fragments in parallel for the specified hot methods.
     *
     * <p>This method iterates through all hot compilation units in the experiment. For each unit, it
     * submits a task to the {@link ExecutorService} to traverse the compilation tree (which can be
     * IO-intensive for AOT). If a node in the tree matches a name in {@code hotMethodNames}, a
     * fragment is created.
     *
     * @param experiment The experiment to process.
     * @param options Options checking if fragment creation is enabled.
     * @param hotMethodNames The set of method names considered hot (e.g., union of two runs).
     */
    private boolean createFragmentsParallel(Experiment experiment, OptionValues options, Set<String> hotMethodNames) {
        if (!options.shouldCreateFragments()) {
            return true;
        }

        List<Callable<Void>> tasks = new ArrayList<>();
        var selfHotMap = experiment.getHotMethodsByName();
        for (Method method : selfHotMap.getValues()) {
            tasks.add(() -> {
                method.createFragments(hotMethodNames::contains);
                return null;
            });
        }

        try {
            List<Future<Void>> futures = fragmentExecutor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Fragment generation interrupted", e);
            return false;
        } catch (ExecutionException e) {
            LOGGER.error("Parallel fragment generation failed due to an underlying error", e.getCause());
            return false;
        }
    }

    private ArrayList<MethodComparisonPair> getMethodsUnion(
            ExperimentResult experimentResult1, ExperimentResult experimentResult2) {
        Set<String> allMethodNames = new HashSet<>();
        for (var m : experimentResult1.experiment().getMethodsByName().getValues()) {
            allMethodNames.add(m.getMethodName());
        }
        for (var m : experimentResult2.experiment().getMethodsByName().getValues()) {
            allMethodNames.add(m.getMethodName());
        }

        var resultPairs = new ArrayList<MethodComparisonPair>();
        for (String methodName : allMethodNames) {
            var m1 = experimentResult1.experiment().getMethodsByName().get(methodName);
            var m2 = experimentResult2.experiment().getMethodsByName().get(methodName);

            boolean hasUnits1 = m1 != null && !m1.getCompilationUnits().isEmpty();
            boolean hasUnits2 = m2 != null && !m2.getCompilationUnits().isEmpty();

            if (hasUnits1 || hasUnits2) {
                resultPairs.add(new MethodComparisonPair(
                        hasUnits1 ? ProfdiffMapper.mapMethod(m1) : null,
                        hasUnits2 ? ProfdiffMapper.mapMethod(m2) : null));
            }
        }
        return resultPairs;
    }

    private List<WarningMessage> getComparisonWarnings(OptionValues options, Path path1, Path path2)
            throws RunsFileParsingException, ResourceNotFoundException, ProfdiffProcessingException {
        var pair = getPreparedCompareExperimentPair(path1, path2, options);
        List<WarningMessage> allWarnings = new ArrayList<>();

        pair.result1()
                .warnings()
                .forEach(msg ->
                        allWarnings.add(WarningMessage.of("[" + path1.getFileName() + "] " + msg, "PARSER_WARNING")));
        pair.result2()
                .warnings()
                .forEach(msg ->
                        allWarnings.add(WarningMessage.of("[" + path2.getFileName() + "] " + msg, "PARSER_WARNING")));

        return allWarnings;
    }
}
