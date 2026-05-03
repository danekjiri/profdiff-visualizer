package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Union of comparative Java methods' across two different profiling runs.
 *
 * @param methodName The identifier of the method being compared.
 * @param methodFromRun1 The method captured in the first profiling run (nullable).
 * @param methodFromRun2 The method captured in the second profiling run (nullable).
 */
@Introspected
public record MethodComparisonPair(
        @Schema(
                        description = "The identifier of the method being compared.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String methodName,
        @Schema(
                        description = "The method captured in the first profiling run (nullable).",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                JavaMethod methodFromRun1,
        @Schema(
                        description = "The method captured in the second profiling run (nullable).",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                JavaMethod methodFromRun2) {

    /** Ctor that resolves method name from both methods passed */
    public MethodComparisonPair(JavaMethod methodFromRun1, JavaMethod methodFromRun2) {
        this(resolveName(methodFromRun1, methodFromRun2), methodFromRun1, methodFromRun2);
    }

    private static String resolveName(JavaMethod m1, JavaMethod m2) {
        if (m1 == null && m2 == null) {
            throw new IllegalArgumentException("At least one method must be provided to resolve a name.");
        }

        if (m1 != null && m2 != null && !m1.name().equals(m2.name())) {
            throw new IllegalArgumentException("The method name differs for passed methods.");
        }

        String resolvedName = (m1 != null) ? m1.name() : m2.name();
        if (resolvedName == null) {
            throw new IllegalArgumentException("The resolved method name cannot be null.");
        }

        return resolvedName;
    }
}
