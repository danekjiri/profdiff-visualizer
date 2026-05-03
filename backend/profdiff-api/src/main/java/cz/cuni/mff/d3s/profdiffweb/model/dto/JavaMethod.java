package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.graalvm.profdiff.core.Method;

/**
 * A named method with a list of compilation units and a total execution period.
 *
 * <p>It is used to encapsulate subset of relevant data from Profdiff's {@link Method}.
 *
 * @param name A stable method name defined by the GraalVM compiler.
 * @param compilationUnitMetadata List of compilation units associated with this method.
 * @param totalPeriod Total sampling period associated with a method (only if profiler file
 *     available).
 */
@Introspected
public record JavaMethod(
        @Schema(
                        description = "A stable method name defined by the GraalVM compiler.",
                        example = "java.util.HashMap.computeIfAbsent(java.lang.Object, java.util.function.Function)",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @Schema(
                        description = "List of compilation units associated with this method.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                List<CompilationUnitMetadata> compilationUnitMetadata,
        @Schema(
                        description =
                                "Total sampling period associated with a method (only if profiler file available).",
                        example = "353081587027",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                long totalPeriod) {}
