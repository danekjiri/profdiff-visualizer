package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a method that is among the top methods by execution time.
 *
 * <p>It encapsulates subset of relevant data from {@link org.graalvm.profdiff.core.ProftoolMethod}.
 * It is used to display a table of top hot methods in UI.
 *
 * <p>If no profiler information is available, the table will be empty. (not shown at all in UI).
 *
 * @param executionPercentage Percentage of total execution time attributed to this method.
 * @param cycles Number of cycles (in billions) attributed to this method.
 * @param level Compiler optimization level of the method (0-4).
 * @param id Unique identifier of the method, if the method is interpreter or stub, then null.
 * @param name Full name with arguments types of the method.
 */
@Introspected
public record TopMethod(
        @Schema(
                        description = "Percentage of total execution time attributed to this method.",
                        example = "7.01%",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String executionPercentage,
        @Schema(
                        description = "Number of cycles (in billions) attributed to this method.",
                        example = "24.68",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String cycles,
        @Schema(
                        description = "Compiler optimization level of the method (0-4).",
                        example = "4",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Integer level,
        @Schema(
                        description =
                                "Unique identifier of the method, if the method is interpreter or stub, then null.",
                        example = "24386",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String id,
        @Schema(
                        description = "Full name with arguments types of the method.",
                        example = "java.util.HashMap.computeIfAbsent(java.lang.Object, java.util.function.Function)",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name) {}
