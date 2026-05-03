package cz.cuni.mff.d3s.profdiffweb.port.profdiff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.ProfdiffMapper;
import java.util.List;
import org.graalvm.collections.EconomicMap;
import org.graalvm.profdiff.core.*;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.graalvm.profdiff.core.optimization.Optimization;
import org.graalvm.profdiff.core.optimization.OptimizationTreeNode;
import org.graalvm.profdiff.core.optimization.Position;
import org.junit.jupiter.api.Test;

class ProfdiffMapperTest {

    @Test
    void toOptionValues_shouldMapFlagsCorrectlyAndHandleNulls() {
        var renderingOptions = new ExperimentProcessingOptions(true, false, true, null, true, false, true);
        var policy = new HotCompilationUnitPolicy();

        OptionValues result = ProfdiffMapper.toOptionValues(renderingOptions, policy);

        assertTrue(result.isBCILongForm());
        assertFalse(result.shouldSortInliningTree());
        assertTrue(result.shouldSortUnorderedPhases());
        assertFalse(result.shouldRemoveVeryDetailedPhases());
        assertTrue(result.shouldPruneIdentities());
        assertFalse(result.shouldCreateFragments());
        assertTrue(result.shouldAlwaysPrintInlinerReasoning());
    }

    @Test
    void toHotPolicy_shouldMapLimitsAndPercentiles() {
        var dto = new HotPolicyOptions(10, 100, 0.95);
        HotCompilationUnitPolicy policy = ProfdiffMapper.toHotPolicy(dto);

        assertEquals(10, policy.getHotMinLimit());
        assertEquals(100, policy.getHotMaxLimit());
        assertEquals(0.95, policy.getHotPercentile());
    }

    @Test
    void mapTopMethod_shouldFormatExecutionPercentageAndCyclesCorrectly() {
        var proftoolMethod = mock(ProftoolMethod.class);
        when(proftoolMethod.getPeriod()).thenReturn(250_000_000L);
        when(proftoolMethod.getLevel()).thenReturn(4);
        when(proftoolMethod.getCompilationId()).thenReturn("comp1");
        when(proftoolMethod.getName()).thenReturn("methodName");

        long totalPeriod = 1_000_000_000L;

        TopMethod result = ProfdiffMapper.mapTopMethod(proftoolMethod, totalPeriod);

        assertEquals("25.00%", result.executionPercentage());
        assertEquals("0.25", result.cycles());
        assertEquals(4, result.level());
        assertEquals("comp1", result.id());
        assertEquals("methodName", result.name());
    }

    @Test
    void mapMethod_shouldMapCompilationUnitsCorrectly() {
        var profdiffMethod = mock(Method.class);
        var compilationUnit = mock(CompilationUnit.class);
        var fragment = mock(CompilationFragment.class);

        when(profdiffMethod.getMethodName()).thenReturn("methodName");
        when(profdiffMethod.getTotalPeriod()).thenReturn(500L);
        when(profdiffMethod.getCompilationUnits()).thenReturn(List.of(compilationUnit, fragment));

        when(compilationUnit.getCompilationId()).thenReturn("comp1");
        when(compilationUnit.getPeriod()).thenReturn(200L);
        when(compilationUnit.isHot()).thenReturn(false);

        when(fragment.getCompilationId()).thenReturn("comp2");
        when(fragment.getPeriod()).thenReturn(300L);
        when(fragment.isHot()).thenReturn(true);

        JavaMethod result = ProfdiffMapper.mapMethod(profdiffMethod);

        assertEquals("methodName", result.name());
        assertEquals(500L, result.totalPeriod());
        assertEquals(2, result.compilationUnitMetadata().size());

        assertEquals("comp1", result.compilationUnitMetadata().get(0).id());
        assertFalse(result.compilationUnitMetadata().get(0).isFragment());

        assertEquals("comp2", result.compilationUnitMetadata().get(1).id());
        assertTrue(result.compilationUnitMetadata().get(1).isHot());
        assertTrue(result.compilationUnitMetadata().get(1).isFragment());
    }

    @Test
    void mapRunMetadata_shouldAggregateCountsProperly() {
        var baseMetadata = mock(BenchmarkRunMetadata.class);
        var experiment = mock(Experiment.class);

        var method1 = mock(Method.class);
        var method2 = mock(Method.class);
        when(method1.getCompilationUnits()).thenReturn(List.of(mock(CompilationUnit.class)));
        when(method2.getCompilationUnits())
                .thenReturn(List.of(mock(CompilationUnit.class), mock(CompilationUnit.class)));

        EconomicMap<String, Method> methodsMap = EconomicMap.create();
        methodsMap.put("methodName1", method1);
        methodsMap.put("methodName2", method2);

        when(experiment.getMethodsByName()).thenReturn(methodsMap);
        when(experiment.getExecutionId()).thenReturn("exp1");
        when(experiment.getTotalPeriod()).thenReturn(1000L);
        when(experiment.getGraalPeriod()).thenReturn(500L);

        RunMetadata result = ProfdiffMapper.mapRunMetadata(baseMetadata, experiment);

        assertEquals("exp1", result.executionId());
        assertEquals(1000L, result.totalPeriod());
        assertEquals(500L, result.graalPeriod());
        assertEquals(2, result.proftoolMethodsCount());
        assertEquals(3, result.compilationUnitsCount());
        assertEquals(baseMetadata, result.benchmarkRunMetadata());
    }

    @Test
    void createRawContent_shouldSetRawTextOnly() {
        RenderedTreeNode.NodeContent result = ProfdiffMapper.createRawContent("Fallback Content");

        assertEquals("Fallback Content", result.rawText());
        assertNull(result.action());
        assertNull(result.methodName());
        assertNull(result.bci());
        assertNull(result.additionalInfo());
    }

    @Test
    void buildInliningContent_shouldExtractActionMethodNameAndBci() {
        var mockNode = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(mockNode.getCallsiteKind().prefix()).thenReturn("(inlined)");
        when(mockNode.getName()).thenReturn("myMethod");
        when(mockNode.getBCI()).thenReturn(42);

        var result = ProfdiffMapper.buildInliningContent(mockNode);

        assertEquals("(inlined)", result.action());
        assertEquals("myMethod", result.methodName());
        assertEquals("42", result.bci());
        assertNull(result.additionalInfo());
        assertNull(result.rawText());
    }

    @Test
    void buildInliningContent_shouldHandleUnknownNamesAndBci() {
        var mockNode = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(mockNode.getCallsiteKind().prefix()).thenReturn("");
        when(mockNode.getName()).thenReturn(null);
        when(mockNode.getBCI()).thenReturn(-1);

        var result = ProfdiffMapper.buildInliningContent(mockNode);

        assertEquals("", result.action());
        assertEquals(InliningTreeNode.UNKNOWN_NAME, result.methodName());
        assertNull(result.bci());
    }

    @Test
    void buildInliningContent_withZeroBci_shouldReturnZeroString() {
        var mockNode = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(mockNode.getCallsiteKind().prefix()).thenReturn("(inlined)");
        when(mockNode.getName()).thenReturn("rootMethod");
        when(mockNode.getBCI()).thenReturn(0);

        var result = ProfdiffMapper.buildInliningContent(mockNode);

        assertEquals("0", result.bci(), "BCI of 0 is a valid index and should be mapped as a string.");
    }

    @Test
    void buildOptimizationContent_shouldExtractAllFieldsCorrectly() {
        var mockNode = mock(Optimization.class);
        var mockOptions = mock(OptionValues.class);
        var mockPosition = mock(Position.class);

        when(mockNode.getName()).thenReturn("Phase");
        when(mockNode.getEventName()).thenReturn("Started");
        when(mockPosition.toString(anyBoolean(), any())).thenReturn("bci:10");
        when(mockNode.getPosition()).thenReturn(mockPosition);
        when(mockOptions.isBCILongForm()).thenReturn(false);

        EconomicMap<String, Object> properties = EconomicMap.create();
        properties.put("peelings", 1);
        properties.put("status", "success");
        when(mockNode.getProperties()).thenReturn(properties);

        var result = ProfdiffMapper.buildOptimizationContent(mockNode, mockOptions);

        assertEquals("Phase Started", result.action());
        assertEquals("bci:10", result.bci());
        assertEquals("{peelings: 1, status: \"success\"}", result.additionalInfo());
        assertNull(result.methodName());
        assertNull(result.rawText());
    }

    @Test
    void buildOptimizationContent_shouldHandleNullFields() {
        var mockNode = mock(Optimization.class);
        var mockOptions = mock(OptionValues.class);

        when(mockNode.getName()).thenReturn("Phase");
        when(mockNode.getEventName()).thenReturn(null);
        when(mockNode.getPosition()).thenReturn(null);
        when(mockNode.getProperties()).thenReturn(null);

        var result = ProfdiffMapper.buildOptimizationContent(mockNode, mockOptions);

        assertEquals("Phase", result.action());
        assertNull(result.bci());
        assertNull(result.additionalInfo());
    }

    @Test
    void buildOptimizationContent_withEmptyProperties_shouldNotIncludeProps() {
        var mockNode = mock(Optimization.class);
        var mockOptions = mock(OptionValues.class);

        when(mockNode.getName()).thenReturn("Phase");
        when(mockNode.getProperties()).thenReturn(EconomicMap.create());

        var result = ProfdiffMapper.buildOptimizationContent(mockNode, mockOptions);

        assertNull(result.additionalInfo(), "Empty properties map should not be mapped to a string.");
    }

    @Test
    void createNodeContent_shouldReturnNullForNullNode() {
        assertNull(ProfdiffMapper.createNodeContent(null, mock(OptionValues.class)));
    }

    @Test
    void createNodeContent_shouldReturnRawTextForInfoNode() {
        var mockOptions = mock(OptionValues.class);
        var mockNode = mock(TreeNode.class);
        when(mockNode.isInfoNode()).thenReturn(true);
        when(mockNode.getName()).thenReturn("Info String");

        var result = ProfdiffMapper.createNodeContent(mockNode, mockOptions);
        assertEquals("Info String", result.rawText());
    }

    @Test
    void createRelabelContent_withInliningTreeNodes_shouldMergeActions() {
        var mockOptions = mock(OptionValues.class);

        var leftNode = mock(InliningTreeNode.class);
        when(leftNode.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Indirect);
        when(leftNode.getName()).thenReturn("MyMethod");
        when(leftNode.getBCI()).thenReturn(15);

        var rightNode = mock(InliningTreeNode.class);
        when(rightNode.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Inlined);

        var result = ProfdiffMapper.createRelabelContent(leftNode, rightNode, mockOptions);

        assertEquals("(indirect -> inlined)", result.action());
        assertEquals("MyMethod", result.methodName());
        assertEquals("15", result.bci());
    }

    @Test
    void createRelabelContent_withOptimizationContextTreeNodes_shouldMergeActions() {
        var mockOptions = mock(OptionValues.class);

        var leftCtx = mock(OptimizationContextTreeNode.class);
        var leftInNode = mock(InliningTreeNode.class);
        when(leftCtx.getOriginalInliningTreeNode()).thenReturn(leftInNode);
        when(leftInNode.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Direct);
        when(leftInNode.getName()).thenReturn("CtxMethod");
        when(leftInNode.getBCI()).thenReturn(30);

        var rightCtx = mock(OptimizationContextTreeNode.class);
        var rightInNode = mock(InliningTreeNode.class);
        when(rightCtx.getOriginalInliningTreeNode()).thenReturn(rightInNode);
        when(rightInNode.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Deleted);

        var result = ProfdiffMapper.createRelabelContent(leftCtx, rightCtx, mockOptions);

        assertEquals("(direct -> deleted)", result.action());
        assertEquals("CtxMethod", result.methodName());
        assertEquals("30", result.bci());
    }

    @Test
    void createRelabelContent_withOtherNodes_shouldFallbackToRightNode() {
        var mockOptions = mock(OptionValues.class);

        var leftNode = mock(OptimizationTreeNode.class);
        var rightNode = mock(OptimizationTreeNode.class);
        when(rightNode.getName()).thenReturn("Optimization Phase");

        var result = ProfdiffMapper.createRelabelContent(leftNode, rightNode, mockOptions);

        assertEquals("Optimization Phase", result.action());
        assertNull(result.methodName());
    }
}
