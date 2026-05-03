package cz.cuni.mff.d3s.profdiffweb.controller.exceptionhandler;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ErrorMessage;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffProcessingException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles wrapped exceptions thrown by the Profdiff library during experiment parsing or tree
 * operations.
 *
 * <p>Returns a 500 Internal Server Error and logs the full stack trace, as this indicates a failure
 * in the underlying processing logic or a library panic.
 */
@Produces
@Singleton
@Requires(classes = {ProfdiffProcessingException.class, ExceptionHandler.class})
public class ProfdiffProcessingErrorHandler
        implements ExceptionHandler<ProfdiffProcessingException, HttpResponse<ErrorMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfdiffProcessingErrorHandler.class);

    @Override
    public HttpResponse<ErrorMessage> handle(HttpRequest request, ProfdiffProcessingException exception) {
        LOGGER.error(
                "[Processing Error] {} {} - {}",
                request.getMethod(),
                request.getPath(),
                exception.getMessage(),
                exception);
        return HttpResponse.serverError(
                ErrorMessage.of("Failed to process experiment data: " + exception.getMessage()));
    }
}
