package cz.cuni.mff.d3s.profdiffweb.port.profdiff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.ExperimentParserService;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache.PreparedExperimentCache;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import cz.cuni.mff.d3s.profdiffweb.service.ResourceNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.graalvm.collections.EconomicMap;
import org.graalvm.profdiff.core.*;
import org.graalvm.profdiff.core.inlining.InliningTree;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.graalvm.profdiff.parser.ExperimentParserError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfdiffPortImplTest {

    @Mock
    private ExperimentParserService experimentParserService;

    @Mock
    private PreparedExperimentCache preparedExperimentCache;

    private ProfdiffPortImpl profdiffPort;
    private final Path mockRunPath = Paths.get("/tmp/runs/run1");
    private final Path mockRunPath2 = Paths.get("/tmp/runs/run2");

    private ExperimentProcessingOptions defaultRenderingOptions;
    private HotPolicyOptions defaultHotPolicy;

    @BeforeEach
    void setUp() {
        profdiffPort = new ProfdiffPortImpl(experimentParserService, preparedExperimentCache);
        defaultRenderingOptions = new ExperimentProcessingOptions(false, false, false, false, false, false, false);
        defaultHotPolicy = new HotPolicyOptions(null, null, null);
    }

    @AfterEach
    void tearDown() {
        profdiffPort.shutdownExecutor();
    }

    @Nested
    class RunMetadataTests {
        @Test
        void getRunMetadata_shouldFetchAndMapMetadata() {
            var mockExperiment = mock(Experiment.class);
            when(mockExperiment.getMethodsByName()).thenReturn(EconomicMap.create());
            var mockResult = new ExperimentResult(mockExperiment, Collections.emptyList());
            when(experimentParserService.getParsedExperimentResult(mockRunPath)).thenReturn(mockResult);

            RunMetadata result = profdiffPort.getRunMetadata(mockRunPath, mock(BenchmarkRunMetadata.class));

            assertNotNull(result, "RunMetadata should not be null.");
            verify(experimentParserService).getParsedExperimentResult(mockRunPath);
        }
    }

    @Nested
    class TopHotMethodsTests {

        @Test
        void getTopHotMethods_noProfileData_shouldReturnEmptyList() {
            var mockExperiment = mock(Experiment.class);
            when(mockExperiment.isProfileAvailable()).thenReturn(false);
            var mockResult = new ExperimentResult(mockExperiment, Collections.emptyList());
            when(experimentParserService.getParsedExperimentResult(mockRunPath)).thenReturn(mockResult);

            List<TopMethod> result = profdiffPort.getTopHotMethods(mockRunPath, defaultHotPolicy);

            assertTrue(result.isEmpty(), "Should return empty list if profile is not available.");
            verify(mockExperiment, never()).getTopMethods(anyInt());
        }

        @Test
        void getTopHotMethods_withProfileData_shouldReturnMappedMethods() {
            var mockExperiment = mock(Experiment.class);
            when(mockExperiment.isProfileAvailable()).thenReturn(true);
            when(mockExperiment.getTotalPeriod()).thenReturn(1000L);
            var mockResult = new ExperimentResult(mockExperiment, Collections.emptyList());
            when(experimentParserService.getParsedExperimentResult(mockRunPath)).thenReturn(mockResult);
            var mockMethod = mock(ProftoolMethod.class);
            when(mockMethod.getName()).thenReturn("org.example.HotMethod");
            when(mockExperiment.getTopMethods(10)).thenReturn(Stream.of(mockMethod));

            List<TopMethod> result = profdiffPort.getTopHotMethods(mockRunPath, defaultHotPolicy);

            assertEquals(1, result.size(), "Should return exactly one mapped method.");
            verify(mockExperiment).getTopMethods(10);
        }
    }

    @Nested
    class CachingAndPreparationTests {

        @Test
        void getCompiledMethods_cacheHit_shouldSkipParser() {
            var mockExperiment = mock(Experiment.class);
            when(mockExperiment.getMethodsByName()).thenReturn(EconomicMap.create());
            var cachedResult = new ExperimentResult(mockExperiment, Collections.emptyList());
            when(preparedExperimentCache.get(any())).thenReturn(Optional.of(cachedResult));

            profdiffPort.getCompiledMethods(mockRunPath, defaultRenderingOptions, defaultHotPolicy);

            verify(experimentParserService, never()).getParsedExperimentResult(any(), any());
        }

        @Test
        void getCompiledMethods_cacheMiss_shouldGenerateFragmentsAndCache() throws ExperimentParserError {
            var mockExperiment = mock(Experiment.class);
            when(mockExperiment.isProfileAvailable()).thenReturn(false);
            when(mockExperiment.getHotMethodsNames()).thenReturn(Set.of("hotMethod1"));
            var hotMethodMap = EconomicMap.<String, Method>create();
            var mockMethod = mock(Method.class);
            hotMethodMap.put("hotMethod1", mockMethod);
            when(mockExperiment.getHotMethodsByName()).thenReturn(hotMethodMap);
            when(mockExperiment.getMethodsByName()).thenReturn(EconomicMap.create());

            var parsedResult = new ExperimentResult(mockExperiment, Collections.emptyList());

            when(preparedExperimentCache.get(any())).thenReturn(Optional.empty());
            when(experimentParserService.getParsedExperimentResult(eq(mockRunPath), any()))
                    .thenReturn(parsedResult);
            var optionsWithFragments = new ExperimentProcessingOptions(false, false, false, false, false, true, true);

            profdiffPort.getCompiledMethods(mockRunPath, optionsWithFragments, defaultHotPolicy);

            verify(experimentParserService).getParsedExperimentResult(eq(mockRunPath), any());
            verify(mockMethod).createFragments(any());
            verify(preparedExperimentCache).put(any(), eq(parsedResult));
        }
    }

    @Nested
    class CompiledMethodsUnionPairsTests {

        @Test
        void getCompiledMethodsUnionPairs_shouldCreateCorrectUnion() {
            var experiment1 = mock(Experiment.class);
            var experiment2 = mock(Experiment.class);

            var map1 = EconomicMap.<String, Method>create();
            var m1 = mock(Method.class);
            var cu1 = mock(CompilationUnit.class);
            when(m1.getMethodName()).thenReturn("SharedMethod");
            when(m1.getCompilationUnits()).thenReturn(List.of(cu1));
            map1.put("SharedMethod", m1);

            var map2 = EconomicMap.<String, Method>create();
            var m2 = mock(Method.class);
            var cu2 = mock(CompilationUnit.class);
            when(m2.getMethodName()).thenReturn("SharedMethod");
            when(m2.getCompilationUnits()).thenReturn(List.of(cu2));
            map2.put("SharedMethod", m2);

            var mOnlyIn2 = mock(Method.class);
            var cuOnlyIn2 = mock(CompilationUnit.class);
            when(mOnlyIn2.getMethodName()).thenReturn("OnlyInRun2");
            when(mOnlyIn2.getCompilationUnits()).thenReturn(List.of(cuOnlyIn2));
            map2.put("OnlyInRun2", mOnlyIn2);

            when(experiment1.getMethodsByName()).thenReturn(map1);
            when(experiment2.getMethodsByName()).thenReturn(map2);

            var result1 = new ExperimentResult(experiment1, Collections.emptyList());
            var result2 = new ExperimentResult(experiment2, Collections.emptyList());

            when(preparedExperimentCache.get(any()))
                    .thenReturn(Optional.of(result1))
                    .thenReturn(Optional.of(result2));

            var pairs = profdiffPort.getCompiledMethodsUnionPairs(
                    mockRunPath, mockRunPath2, defaultRenderingOptions, defaultHotPolicy);

            assertEquals(2, pairs.size(), "Should have two unique methods in the union.");

            var sharedPair = pairs.stream()
                    .filter(p -> p.methodFromRun1() != null && p.methodFromRun2() != null)
                    .findFirst();
            assertTrue(sharedPair.isPresent(), "Shared method pair should be present.");

            var onlyIn2Pair = pairs.stream()
                    .filter(p -> p.methodFromRun1() == null && p.methodFromRun2() != null)
                    .findFirst();
            assertTrue(onlyIn2Pair.isPresent(), "Method only in run 2 should map left side to null.");
        }
    }

    @Nested
    class MissingResourceAndExceptionTests {
        private Experiment mockExperiment;

        @BeforeEach
        void setupMissingResourceContext() {
            mockExperiment = mock(Experiment.class);
            var mockResult = new ExperimentResult(mockExperiment, Collections.emptyList());

            when(preparedExperimentCache.get(any())).thenReturn(Optional.empty());
            when(experimentParserService.getParsedExperimentResult(eq(mockRunPath), any()))
                    .thenReturn(mockResult);
        }

        @Test
        void getReportInliningTree_missingMethod_shouldThrowResourceNotFoundException() {
            when(mockExperiment.getMethodsByName()).thenReturn(EconomicMap.create());
            when(mockExperiment.getHotMethodsNames()).thenReturn(Collections.emptySet());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> profdiffPort.getReportInliningTree(
                            mockRunPath, "non.existent.Method", "123", defaultRenderingOptions, defaultHotPolicy));

            assertTrue(exception.getMessage().contains("Method 'non.existent.Method' not found."));
        }

        @Test
        void getReportInliningTree_missingCompilationUnit_shouldThrowResourceNotFoundException() {
            var method = mock(Method.class);
            var unit = mock(CompilationUnit.class);
            when(unit.getCompilationId()).thenReturn("comp1");
            when(method.getCompilationUnits()).thenReturn(List.of(unit));

            EconomicMap<String, Method> methodsMap = EconomicMap.create();
            methodsMap.put("methodName", method);

            when(mockExperiment.getMethodsByName()).thenReturn(methodsMap);
            when(mockExperiment.getHotMethodsNames()).thenReturn(Collections.emptySet());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> profdiffPort.getReportInliningTree(
                            mockRunPath, "methodName", "non-existent-comp", defaultRenderingOptions, defaultHotPolicy));

            assertTrue(exception.getMessage().contains("Compilation ID 'non-existent-comp' not found."));
        }

        @Test
        void getReportInliningTree_parserThrowsError_shouldThrowProfdiffProcessingException() throws Exception {
            var method = mock(Method.class);
            var unit = mock(CompilationUnit.class);
            when(unit.getCompilationId()).thenReturn("comp1");

            when(unit.loadInliningTree())
                    .thenThrow(new ProfdiffProcessingException("Error while loading Inlining tree."));
            when(method.getCompilationUnits()).thenReturn(List.of(unit));

            EconomicMap<String, Method> methodsMap = EconomicMap.create();
            methodsMap.put("methodName", method);

            when(mockExperiment.getMethodsByName()).thenReturn(methodsMap);
            when(mockExperiment.getHotMethodsNames()).thenReturn(Collections.emptySet());

            ProfdiffProcessingException exception = assertThrows(
                    ProfdiffProcessingException.class,
                    () -> profdiffPort.getReportInliningTree(
                            mockRunPath, "methodName", "comp1", defaultRenderingOptions, defaultHotPolicy));

            assertTrue(
                    exception.getMessage().contains("Error while loading Inlining tree."),
                    "Message should contain the parser error details.");
        }
    }

    @Nested
    class DiffingTests {

        @Test
        void getComparedInliningTree_success_shouldReturnDiffedTree() throws Exception {
            var experiment1 = mock(Experiment.class);
            var experiment2 = mock(Experiment.class);

            var result1 = new ExperimentResult(experiment1, Collections.emptyList());
            var result2 = new ExperimentResult(experiment2, Collections.emptyList());

            when(preparedExperimentCache.get(any()))
                    .thenReturn(Optional.of(result1))
                    .thenReturn(Optional.of(result2));

            var m1 = mock(Method.class);
            var cu1 = mock(CompilationUnit.class);
            var tree1 = mock(InliningTree.class);
            var root1 = mock(InliningTreeNode.class);
            when(tree1.getRoot()).thenReturn(root1);
            when(root1.getChildren()).thenReturn(Collections.emptyList());
            when(root1.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Root);
            when(cu1.getCompilationId()).thenReturn("comp1");
            when(cu1.loadInliningTree()).thenReturn(tree1);
            when(m1.getCompilationUnits()).thenReturn(List.of(cu1));

            var m2 = mock(Method.class);
            var cu2 = mock(CompilationUnit.class);
            var tree2 = mock(InliningTree.class);
            var root2 = mock(InliningTreeNode.class);
            when(tree2.getRoot()).thenReturn(root2);
            when(root2.getChildren()).thenReturn(Collections.emptyList());
            when(root2.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Root);
            when(cu2.getCompilationId()).thenReturn("comp2");
            when(cu2.loadInliningTree()).thenReturn(tree2);
            when(m2.getCompilationUnits()).thenReturn(List.of(cu2));

            var map1 = EconomicMap.<String, Method>create();
            map1.put("methodName", m1);
            var map2 = EconomicMap.<String, Method>create();
            map2.put("methodName", m2);

            when(experiment1.getMethodsByName()).thenReturn(map1);
            when(experiment2.getMethodsByName()).thenReturn(map2);

            TreeResponse response = profdiffPort.getComparedInliningTree(
                    mockRunPath,
                    mockRunPath2,
                    "methodName",
                    "comp1",
                    "comp2",
                    defaultRenderingOptions,
                    defaultHotPolicy);

            assertNotNull(response);
            verify(tree1).preprocess(any());
            verify(tree2).preprocess(any());
        }
    }
}
