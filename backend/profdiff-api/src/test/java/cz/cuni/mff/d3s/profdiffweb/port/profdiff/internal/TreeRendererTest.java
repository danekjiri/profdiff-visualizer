package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode;
import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode.Marker;
import java.util.Collections;
import org.graalvm.profdiff.core.OptimizationContextTreeNode;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.Writer;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeRendererTest {

    private OptionValues mockOptions;

    @BeforeEach
    void setUp() {
        mockOptions = mock(OptionValues.class);
    }

    @Test
    void convert_withNullRoot_shouldReturnEmptyNeutralNode() {
        RenderedTreeNode result = TreeRenderer.convert(null, mockOptions);

        assertNotNull(result, "Result should not be null even if root is null.");
        assertEquals("(empty)", result.content().rawText(), "Label should default to (empty) for null roots.");
        assertEquals(Marker.NEUTRAL, result.marker(), "Marker should default to NEUTRAL.");
        assertTrue(result.children().isEmpty(), "Null root should have no children.");
    }

    @Test
    void convert_withStandardNode_shouldExtractContentAndMapChildren() {
        var parentMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        var childMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);

        when(parentMock.getName()).thenReturn("ParentMethod");
        when(parentMock.getCallsiteKind().prefix()).thenReturn("(inlined)");
        when(childMock.getName()).thenReturn("ChildMethod");
        when(childMock.getCallsiteKind().prefix()).thenReturn("(inlined)");

        when(parentMock.getChildren()).thenReturn(Collections.singletonList(childMock));
        when(childMock.getChildren()).thenReturn(Collections.emptyList());

        RenderedTreeNode result = TreeRenderer.convert(parentMock, mockOptions);

        assertEquals("ParentMethod", result.content().methodName(), "Parent method name must be correctly extracted.");
        assertEquals(Marker.NEUTRAL, result.marker(), "Standard nodes are NEUTRAL in identity trees.");
        assertEquals(1, result.children().size(), "Should correctly map one child.");
        assertEquals(
                "ChildMethod",
                result.children().getFirst().content().methodName(),
                "Child method name must be correctly extracted.");
    }

    @Test
    void convert_withInfoNode_shouldAssignContextMarker() {
        var infoMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(infoMock.isInfoNode()).thenReturn(true);
        when(infoMock.getName()).thenReturn("Profile Metadata");
        when(infoMock.getChildren()).thenReturn(Collections.emptyList());

        RenderedTreeNode result = TreeRenderer.convert(infoMock, mockOptions);

        assertEquals("Profile Metadata", result.content().rawText(), "Raw text should be extracted for info nodes.");
        assertEquals(Marker.INFO, result.marker(), "Info nodes must be assigned the INFO marker.");
    }

    @Test
    void convert_withInliningNode_shouldExtractContextChildren() {
        var inliningMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(inliningMock.getName()).thenReturn("MethodName");
        when(inliningMock.getCallsiteKind().prefix()).thenReturn("(inlined)");
        when(inliningMock.getChildren()).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
                    ((Writer) inv.getArgument(0)).writeln("Reasoning: bytecode parser did not replace invoke");
                    return null;
                })
                .when(inliningMock)
                .writeReasoningIfEnabled(any(), any());

        RenderedTreeNode result = TreeRenderer.convert(inliningMock, mockOptions);

        assertEquals("MethodName", result.content().methodName(), "Main method name should be extracted.");
        assertEquals(1, result.children().size(), "The reasoning line should be added as a virtual child.");

        RenderedTreeNode virtualChild = result.children().getFirst();
        assertEquals(
                "Reasoning: bytecode parser did not replace invoke",
                virtualChild.content().rawText(),
                "Virtual child should carry the reasoning text in rawText.");
        assertEquals(Marker.INFO, virtualChild.marker(), "Virtual context children must use INFO marker.");
    }

    @Test
    void convert_withOptimizationContextNode_shouldExtractWrappedReasoning() {
        var ctxMock = mock(OptimizationContextTreeNode.class);
        var innerInliningMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);

        when(ctxMock.getChildren()).thenReturn(Collections.emptyList());
        when(ctxMock.getOriginalInliningTreeNode()).thenReturn(innerInliningMock);
        when(innerInliningMock.getName()).thenReturn("WrappedMethod");
        when(innerInliningMock.getCallsiteKind().prefix()).thenReturn("(inlined)");

        doAnswer(inv -> {
                    ((Writer) inv.getArgument(0)).writeln("Profile: Hot branch");
                    return null;
                })
                .when(innerInliningMock)
                .writeReceiverTypeProfile(any(), any());

        RenderedTreeNode result = TreeRenderer.convert(ctxMock, mockOptions);

        assertEquals(1, result.children().size(), "Reasoning should pierce the OptimizationContextTreeNode wrapper.");

        RenderedTreeNode virtualChild = result.children().getFirst();
        assertEquals("Profile: Hot branch", virtualChild.content().rawText());
        assertEquals(Marker.INFO, virtualChild.marker());
    }
}
