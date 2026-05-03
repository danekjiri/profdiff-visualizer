package cz.cuni.mff.d3s.profdiffweb.port.profdiff.model;

import io.micronaut.core.annotation.Introspected;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.profdiff.core.Experiment;

/**
 * A wrapper record that holds a parsed Profdiff {@link Experiment} along with any parser warnings.
 *
 * @param experiment The parsed experiment containing benchmark run data.
 * @param warnings A list of warning messages generated during the parsing process.
 */
@Introspected
public record ExperimentResult(Experiment experiment, List<String> warnings) {
    public ExperimentResult deepCopy() {
        var copiedWarnings = new ArrayList<>(this.warnings());
        return new ExperimentResult(this.experiment().deepCopy(), copiedWarnings);
    }
}
