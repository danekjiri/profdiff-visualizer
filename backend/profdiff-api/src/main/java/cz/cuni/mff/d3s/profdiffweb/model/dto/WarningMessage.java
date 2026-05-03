package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a non-fatal warning that occurred during the processing or parsing of a benchmark run
 * (runs metadata or the whole experiment with profdiff).
 *
 * @param message The human-readable description of the warning.
 * @param type A programmatic category identifier used for UI styling or filtering.
 */
@Introspected
public record WarningMessage(
        @Schema(
                        description = "The human-readable description of the warning",
                        example = "Bench-results.json not found",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String message,
        @Schema(
                        description = "A programmatic category identifier used for UI styling or filtering",
                        example = "PROFILER_MISSING",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String type) {

    /**
     * Factory method to create a warning message with a specific type.
     *
     * @param message The human-readable description of the warning.
     * @param type A programmatic category identifier used for UI styling or filtering.
     */
    public static WarningMessage of(String message, String type) {
        return new WarningMessage(message, type);
    }

    /**
     * Factory method to create a warning message with the default "GENERAL" type.
     *
     * @param message The human-readable description of the warning.
     */
    public static WarningMessage of(String message) {
        return new WarningMessage(message, "GENERAL");
    }
}
