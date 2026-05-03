package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.Writer;

/**
 * A custom implementation of Profdiff's {@link Writer} that captures output line-by-line.
 *
 * <p>Unlike the default {@link StringBuilderWriter} which accumulates the entire tree's output into
 * a single massive string, this writer allows extracting and clearing the buffer after rendering an
 * individual node. This is essential for converting Profdiff's sequential AST into a structured
 * JSON hierarchy for data transfer.
 *
 * <p>If upstream callers invoke {@code write()} without ever calling {@code writeln()}, the output will
 * accumulate in the buffer and only flush on the next call to {@link #takeLines()}.
 */
public class LineCapturingWriter extends Writer {
    private final List<String> lines = new ArrayList<>();
    private final StringBuilder currentLine = new StringBuilder();

    public LineCapturingWriter(OptionValues options) {
        super(options);
    }

    @Override
    protected void writeImpl(String output) {
        currentLine.append(output);
    }

    @Override
    protected void writelnImpl() {
        lines.add(currentLine.toString());
        currentLine.setLength(0);
    }

    /** Gets the captured lines and resets the writer for the next node. */
    public List<String> takeLines() {
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
            currentLine.setLength(0);
        }
        List<String> result = new ArrayList<>(lines);
        lines.clear();
        return result;
    }
}
