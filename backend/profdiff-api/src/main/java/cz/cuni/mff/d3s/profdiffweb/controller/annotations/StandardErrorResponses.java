package cz.cuni.mff.d3s.profdiffweb.controller.annotations;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ErrorMessage;
import io.micronaut.http.MediaType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Meta-annotation to apply standard 404 and 500 error responses to API endpoints. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ApiResponses(
        value = {
            @ApiResponse(
                    responseCode = "404",
                    description =
                            "The provided path was invalid, the specified runs or methods were not found, or a directory-related error occurred.",
                    content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "An unexpected internal server error occurred while processing the request.",
                    content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorMessage.class)))
        })
public @interface StandardErrorResponses {}
