package cz.cuni.mff.d3s.profdiffweb.controller.exceptionhandler;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ErrorMessage;
import cz.cuni.mff.d3s.profdiffweb.service.ResourceNotFoundException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles exceptions related to missing resources (directories, runs, methods, or compilation
 * units).
 *
 * <p>Returns a 404 Not Found. Uses WARN logging because this is typically a client error requesting
 * an entity that doesn't exist, not a server crash.
 */
@Produces
@Singleton
@Requires(classes = {ResourceNotFoundException.class, ExceptionHandler.class})
public class ResourceNotFoundExceptionHandler
        implements ExceptionHandler<ResourceNotFoundException, HttpResponse<ErrorMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNotFoundExceptionHandler.class);

    @Override
    public HttpResponse<ErrorMessage> handle(HttpRequest request, ResourceNotFoundException exception) {
        LOGGER.warn("[Not Found] {} {} - {}", request.getMethod(), request.getPath(), exception.getMessage());
        return HttpResponse.notFound(ErrorMessage.of(exception.getMessage()));
    }
}
