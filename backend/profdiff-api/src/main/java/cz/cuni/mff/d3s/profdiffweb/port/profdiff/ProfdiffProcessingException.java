package cz.cuni.mff.d3s.profdiffweb.port.profdiff;

import cz.cuni.mff.d3s.profdiffweb.service.ProfdiffWebException;

/**
 * Exception thrown for errors originating from the underlying Profdiff CLI library.
 *
 * <p>This includes parser panics, missing methods, missing compilation units, and tree generation
 * failures during the diffing or processing workflow.
 */
public class ProfdiffProcessingException extends ProfdiffWebException {
    public ProfdiffProcessingException(String message) {
        super(message);
    }

    public ProfdiffProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
