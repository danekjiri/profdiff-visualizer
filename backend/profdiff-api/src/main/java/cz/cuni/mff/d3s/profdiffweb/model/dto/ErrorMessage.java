package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents an error message that is return within non 200 HTTP responses.
 *
 * <p>It is used in a way so that the error message is always serialized into a consistent format
 * (JSON) by Micronaut.
 *
 * @param message Error message to be returned in the response.
 */
@Introspected
public record ErrorMessage(
        @Schema(
                        description = "Error message to be returned in the response.",
                        example = "Invalid path provided: /invalid/absolute/path.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String message) {

    /**
     * Factory method.
     *
     * @param message The error message to be included in the ErrorMessage.
     */
    public static ErrorMessage of(String message) {
        return new ErrorMessage(message);
    }
}
