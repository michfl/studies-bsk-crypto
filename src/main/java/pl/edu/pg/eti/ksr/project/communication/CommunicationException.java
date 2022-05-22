package pl.edu.pg.eti.ksr.project.communication;

/**
 * Exception thrown by communicator on error.
 */
public class CommunicationException extends Exception {
    public CommunicationException(String message) {
        super(message);
    }
}
