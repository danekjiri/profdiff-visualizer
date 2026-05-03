package cz.cuni.mff.d3s.profdiffweb.service;

/**
 * Exception thrown strictly for file content parsing issues.
 *
 * <p>This is used to indicate problems such as malformed JSON, unexpected Jackson tokens, or
 * structural invalidity within metadata files.
 */
public class RunsFileParsingException extends ProfdiffWebException {
    public RunsFileParsingException(String message) {
        super(message);
    }

    public RunsFileParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
