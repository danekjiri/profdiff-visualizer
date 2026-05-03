package cz.cuni.mff.d3s.profdiffweb.service;

/** Base class for all custom exceptions in the Profdiff Visualizer API. */
public class ProfdiffWebException extends RuntimeException {
    public ProfdiffWebException(String message) {
        super(message);
    }

    public ProfdiffWebException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
