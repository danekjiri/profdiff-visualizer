package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode;
import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode.Marker;
import java.util.Collections;
import java.util.List;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.graalvm.profdiff.diff.DeltaTree;
import org.graalvm.profdiff.diff.DeltaTreeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeltaTreeRendererTest {

    private OptionValues mockOptions;

    @BeforeEach
    void setUp() {
        mockOptions = mock(OptionValues.class);
    }

    @Test
    void convert_withNullRoot_shouldReturnEmptyNeutralNode() {
        DeltaTree<InliningTreeNode> mockDeltaTree = mock(DeltaTree.class);
        when(mockDeltaTree.getRoot()).thenReturn(null);

        RenderedTreeNode result = DeltaTreeRenderer.convert(mockDeltaTree, mockOptions);

        assertNotNull(result, "Result should not be null.");
        assertEquals("(empty)", result.content().rawText(), "Null root should yield empty raw text fallback.");
        assertEquals(Marker.NEUTRAL, result.marker(), "Null root should yield NEUTRAL marker.");
    }

    @Test
    void convertNode_isInsertion_shouldMapToInsertMarker() {
        var deltaTree = buildMockDeltaTree(Marker.INSERT, "Inserted Node");
        RenderedTreeNode result = DeltaTreeRenderer.convert(deltaTree, mockOptions);

        assertEquals("Inserted Node", result.content().methodName(), "Method name should match.");
        assertEquals(Marker.INSERT, result.marker(), "Insertion should map to INSERT marker.");
    }

    @Test
    void convertNode_isDeletion_shouldMapToDeleteMarker() {
        var deltaTree = buildMockDeltaTree(Marker.DELETE, "Deleted Node");
        RenderedTreeNode result = DeltaTreeRenderer.convert(deltaTree, mockOptions);

        assertEquals("Deleted Node", result.content().methodName(), "Method name should match.");
        assertEquals(Marker.DELETE, result.marker(), "Deletion should map to DELETE marker.");
    }

    @Test
    void convertNode_isIdentity_shouldMapToIdentityMarker() {
        var deltaTree = buildMockDeltaTree(Marker.IDENTITY, "Identity Node");
        RenderedTreeNode result = DeltaTreeRenderer.convert(deltaTree, mockOptions);

        assertEquals("Identity Node", result.content().methodName(), "Method name should match.");
        assertEquals(Marker.IDENTITY, result.marker(), "Identity should map to IDENTITY marker.");
    }

    @Test
    void convertNode_isInfoNode_shouldMapToInfoMarker() {
        var deltaTree = buildMockDeltaTree(Marker.INFO, "Info Node");
        RenderedTreeNode result = DeltaTreeRenderer.convert(deltaTree, mockOptions);

        assertEquals("Info Node", result.content().methodName(), "Method name should match.");
        assertEquals(Marker.INFO, result.marker(), "Info node should map to INFO marker.");
    }

    @Test
    void convertNode_fallback_shouldMapToNeutralMarker() {
        var deltaTree = buildMockDeltaTree(Marker.NEUTRAL, "Neutral Node");
        RenderedTreeNode result = DeltaTreeRenderer.convert(deltaTree, mockOptions);

        assertEquals("Neutral Node", result.content().methodName(), "Method name should match.");
        assertEquals(Marker.NEUTRAL, result.marker(), "Fallback node should map to NEUTRAL marker.");
    }

    @Test
    void convertNode_isRelabeling_shouldMapToCombinedContent() {
        DeltaTree<InliningTreeNode> mockTree = mock(DeltaTree.class);
        DeltaTreeNode<InliningTreeNode> mockRoot = mock(DeltaTreeNode.class);

        when(mockTree.getRoot()).thenReturn(mockRoot);
        when(mockRoot.isRelabeling()).thenReturn(true);
        when(mockRoot.getChildren()).thenReturn(Collections.emptyList());

        var leftMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(leftMock.getName()).thenReturn("TargetMethod");
        when(leftMock.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Indirect);
        when(leftMock.getBCI()).thenReturn(10);

        var rightMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(rightMock.getName()).thenReturn("TargetMethod");
        when(rightMock.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Inlined);
        when(rightMock.getBCI()).thenReturn(10);

        when(mockRoot.getLeft()).thenReturn(leftMock);
        when(mockRoot.getRight()).thenReturn(rightMock);

        RenderedTreeNode result = DeltaTreeRenderer.convert(mockTree, mockOptions);

        assertEquals(Marker.RELABEL, result.marker(), "Relabeling should map to RELABEL marker.");
        assertNotNull(result.content(), "Content must be populated.");
        assertEquals("TargetMethod", result.content().methodName());
        assertEquals("(indirect -> inlined)", result.content().action(), "Action should reflect the transition.");
        assertEquals("10", result.content().bci());
    }

    @Test
    void convertNode_withChildren_shouldRecurse() {
        DeltaTree<InliningTreeNode> mockTree = mock(DeltaTree.class);
        DeltaTreeNode<InliningTreeNode> mockRoot = mock(DeltaTreeNode.class);
        DeltaTreeNode<InliningTreeNode> mockChild = mock(DeltaTreeNode.class);

        when(mockTree.getRoot()).thenReturn(mockRoot);
        when(mockRoot.getChildren()).thenReturn(List.of(mockChild));
        when(mockChild.getChildren()).thenReturn(Collections.emptyList());

        var rootInner = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(rootInner.getName()).thenReturn("Root");
        when(mockRoot.getLeft()).thenReturn(rootInner);

        var childInner = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(childInner.getName()).thenReturn("Child");
        when(mockChild.getLeft()).thenReturn(childInner);

        RenderedTreeNode result = DeltaTreeRenderer.convert(mockTree, mockOptions);

        assertEquals("Root", result.content().methodName());
        assertEquals(1, result.children().size(), "Renderer should recursively convert children.");
        assertEquals(
                "Child", result.children().get(0).content().methodName(), "Child node should be correctly parsed.");
    }

    @Test
    void convertNode_nullLeftOnNonInsertion_shouldUseRightForInnerExtraction() {
        DeltaTree<InliningTreeNode> mockTree = mock(DeltaTree.class);
        DeltaTreeNode<InliningTreeNode> mockRoot = mock(DeltaTreeNode.class);
        when(mockTree.getRoot()).thenReturn(mockRoot);

        when(mockRoot.isDeletion()).thenReturn(true);
        when(mockRoot.getChildren()).thenReturn(Collections.emptyList());
        when(mockRoot.getLeft()).thenReturn(null);

        var rightInner = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(mockRoot.getRight()).thenReturn(rightInner);

        RenderedTreeNode result = DeltaTreeRenderer.convert(mockTree, mockOptions);

        assertEquals(Marker.DELETE, result.marker());
        assertNull(result.content(), "Content should be null since getLeft() is null.");

        verify(rightInner).writeReasoningIfEnabled(any(), any());
    }

    @Test
    void convertNode_withInliningNode_shouldExtractReasoningAsInfoChildren() {
        DeltaTree<InliningTreeNode> mockTree = mock(DeltaTree.class);
        DeltaTreeNode<InliningTreeNode> mockRoot = mock(DeltaTreeNode.class);
        when(mockTree.getRoot()).thenReturn(mockRoot);
        when(mockRoot.getChildren()).thenReturn(Collections.emptyList());

        var innerMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(innerMock.getName()).thenReturn("MethodWithReasoning");
        when(mockRoot.getLeft()).thenReturn(innerMock);

        doAnswer(inv -> {
                    ((org.graalvm.profdiff.core.Writer) inv.getArgument(0))
                            .writeln("Reasoning: bytecode parser did not replace invoke");
                    return null;
                })
                .when(innerMock)
                .writeReasoningIfEnabled(any(), any());

        RenderedTreeNode result = DeltaTreeRenderer.convert(mockTree, mockOptions);

        assertEquals("MethodWithReasoning", result.content().methodName());
        assertEquals(1, result.children().size(), "Reasoning should be extracted as an INFO child node.");

        RenderedTreeNode virtualChild = result.children().get(0);
        assertEquals(
                "Reasoning: bytecode parser did not replace invoke",
                virtualChild.content().rawText());
        assertEquals(Marker.INFO, virtualChild.marker());
    }

    private DeltaTree<InliningTreeNode> buildMockDeltaTree(Marker intendedMarker, String methodName) {
        DeltaTree<InliningTreeNode> mockTree = mock(DeltaTree.class);
        DeltaTreeNode<InliningTreeNode> mockRoot = mock(DeltaTreeNode.class);
        when(mockTree.getRoot()).thenReturn(mockRoot);

        when(mockRoot.isInsertion()).thenReturn(intendedMarker == Marker.INSERT);
        when(mockRoot.isDeletion()).thenReturn(intendedMarker == Marker.DELETE);
        when(mockRoot.isIdentity()).thenReturn(intendedMarker == Marker.IDENTITY);
        when(mockRoot.isInfoNode()).thenReturn(intendedMarker == Marker.INFO);
        when(mockRoot.isRelabeling()).thenReturn(intendedMarker == Marker.RELABEL);

        when(mockRoot.getChildren()).thenReturn(Collections.emptyList());

        var innerMock = mock(InliningTreeNode.class, RETURNS_DEEP_STUBS);
        when(innerMock.getName()).thenReturn(methodName);

        if (intendedMarker == Marker.RELABEL) {
            when(innerMock.getCallsiteKind()).thenReturn(InliningTreeNode.CallsiteKind.Inlined);
        } else {
            when(innerMock.getCallsiteKind().prefix()).thenReturn("(mock-action)");
        }

        if (intendedMarker == Marker.INSERT) {
            when(mockRoot.getRight()).thenReturn(innerMock);
        } else {
            when(mockRoot.getLeft()).thenReturn(innerMock);
        }

        return mockTree;
    }
}
