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
 * The fallback exception handler for any uncaught exceptions.
 *
 * <p>Returns a generic 500 Internal Server Error to prevent leaking implementation details to the
 * client, while logging the full exception.
 */
@Produces
@Singleton
@Requires(classes = {Exception.class, ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Exception, HttpResponse<ErrorMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public HttpResponse<ErrorMessage> handle(HttpRequest request, Exception exception) {
        LOGGER.error(
                "[Unhandled Error] {} {} - {}",
                request.getMethod(),
                request.getPath(),
                exception.getMessage(),
                exception);
        return HttpResponse.serverError(ErrorMessage.of("An unexpected internal error occurred. Consult server logs."));
    }
}
