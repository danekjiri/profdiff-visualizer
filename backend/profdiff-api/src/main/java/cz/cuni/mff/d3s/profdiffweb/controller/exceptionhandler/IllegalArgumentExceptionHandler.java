package cz.cuni.mff.d3s.profdiffweb.controller.exceptionhandler;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ErrorMessage;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles standard IllegalArgumentExceptions, typically thrown during input validation (e.g., null
 * or blank paths).
 *
 * <p>Returns a 400 Bad Request to indicate the client sent invalid parameters.
 */
@Produces
@Singleton
@Requires(classes = {IllegalArgumentException.class, ExceptionHandler.class})
public class IllegalArgumentExceptionHandler
        implements ExceptionHandler<IllegalArgumentException, HttpResponse<ErrorMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IllegalArgumentExceptionHandler.class);

    @Override
    public HttpResponse<ErrorMessage> handle(HttpRequest request, IllegalArgumentException exception) {
        LOGGER.warn("[Bad Request] {} {} - {}", request.getMethod(), request.getPath(), exception.getMessage());
        return HttpResponse.badRequest(ErrorMessage.of(exception.getMessage()));
    }
}
