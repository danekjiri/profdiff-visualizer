package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import org.graalvm.profdiff.core.HotCompilationUnitPolicy;

/**
 * DTO for Profdiff's {@link HotCompilationUnitPolicy} options.
 *
 * @param hotMinLimit Minimum limit for hot compilation units.
 * @param hotMaxLimit Maximum limit for hot compilation units.
 * @param hotPercentile Percentile threshold to classify a compilation unit as hot.
 */
@Introspected
public record HotPolicyOptions(
        @Schema(
                        description = "Minimum limit for hot compilation units.",
                        example = "1",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                @PositiveOrZero(message = "hotMinLimit must be zero or a positive number")
                Integer hotMinLimit,
        @Schema(
                        description = "Maximum limit for hot compilation units.",
                        example = "10",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                @PositiveOrZero(message = "hotMinLimit must be zero or a positive number")
                Integer hotMaxLimit,
        @Schema(
                        description = "Percentile threshold to classify a compilation unit as hot.",
                        example = "0.9",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                @DecimalMin(value = "0.0", message = "hotPercentile must be between 0.0 and 1.0")
                @DecimalMax(value = "1.0", message = "hotPercentile must be between 0.0 and 1.0")
                Double hotPercentile) {}
