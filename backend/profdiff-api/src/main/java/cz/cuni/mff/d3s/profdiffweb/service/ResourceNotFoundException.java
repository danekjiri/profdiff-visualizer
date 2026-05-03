package cz.cuni.mff.d3s.profdiffweb.service;

/**
 * Exception thrown when a requested resource (run directory, method, compilation unit, etc.) cannot
 * be found on the server.
 *
 * <p>This maps to an HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends ProfdiffWebException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
