package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.graalvm.profdiff.core.OptionValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LineCapturingWriterTest {

    private LineCapturingWriter writer;

    @BeforeEach
    void setUp() {
        OptionValues mockOptions = mock(OptionValues.class);
        writer = new LineCapturingWriter(mockOptions);
    }

    @Test
    void takeLines_withNoInput_shouldReturnEmptyList() {
        List<String> result = writer.takeLines();
        assertTrue(result.isEmpty(), "Taking lines from an untouched writer should return an empty list.");
    }

    @Test
    void write_shouldBufferWithoutAddingNewline() {
        writer.write("Hello");
        writer.write(" ");
        writer.write("World");

        List<String> result = writer.takeLines();

        assertEquals(1, result.size(), "Buffered string without writeln should form exactly one line.");
        assertEquals("Hello World", result.getFirst(), "The concatenated string should match exactly.");
    }

    @Test
    void writeln_shouldSeparateBufferIntoMultipleLines() {
        writer.writeln("Line 1");
        writer.writeln("Line 2");

        List<String> result = writer.takeLines();

        assertEquals(2, result.size(), "Two writeln calls should produce two separate lines.");
        assertEquals("Line 1", result.get(0), "First line should match.");
        assertEquals("Line 2", result.get(1), "Second line should match.");
    }

    @Test
    void takeLines_shouldClearBufferForSubsequentCalls() {
        writer.writeln("First Payload");
        List<String> result1 = writer.takeLines();

        writer.writeln("Second Payload");
        List<String> result2 = writer.takeLines();

        assertEquals(1, result1.size(), "First extraction should have 1 line.");
        assertEquals("First Payload", result1.getFirst(), "First payload must match.");

        assertEquals(1, result2.size(), "Second extraction should have exactly 1 line (buffer was cleared).");
        assertEquals("Second Payload", result2.getFirst(), "Second payload must match without first payload's ghost.");
    }
}
