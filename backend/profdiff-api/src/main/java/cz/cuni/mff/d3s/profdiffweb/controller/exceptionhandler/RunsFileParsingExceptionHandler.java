package cz.cuni.mff.d3s.profdiffweb.controller.exceptionhandler;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ErrorMessage;
import cz.cuni.mff.d3s.profdiffweb.service.RunsFileParsingException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles exceptions thrown when the application fails to parse expected JSON or metadata files.
 *
 * <p>Returns a 500 Internal Server Error because the failure originates from corrupted or
 * incompatible data files on the server side.
 */
@Produces
@Singleton
@Requires(classes = {RunsFileParsingException.class, ExceptionHandler.class})
public class RunsFileParsingExceptionHandler
        implements ExceptionHandler<RunsFileParsingException, HttpResponse<ErrorMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunsFileParsingExceptionHandler.class);

    @Override
    public HttpResponse<ErrorMessage> handle(HttpRequest request, RunsFileParsingException exception) {
        LOGGER.error(
                "[Data Parsing Error] {} {} - {}",
                request.getMethod(),
                request.getPath(),
                exception.getMessage(),
                exception);
        return HttpResponse.serverError(
                ErrorMessage.of("Failed to parse server data files: " + exception.getMessage()));
    }
}
