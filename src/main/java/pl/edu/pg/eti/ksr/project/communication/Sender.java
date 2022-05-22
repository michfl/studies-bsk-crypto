package pl.edu.pg.eti.ksr.project.communication;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

/**
 * Interface for sending messages to other client.
 * Used for initiating, checking and stopping communication and sending supported formats of messages.
 *
 * All methods send a properly formatted message and are non-blocking.
 */
public interface Sender {

    /**
     * Initiates communication by sending username, public key and challenge text to the other client.
     */
    void initiateCommunication();

    /**
     * Sends communication-stop message to announce to the other client that current communication is no longer active.
     */
    void stopCommunication();

    /**
     * Initiates session by sending session info to the other client.
     * One communication can consist of multiple sessions with different parameters.
     */
    void initiateSession();

    /**
     * Sends text message.
     * @param message text to be sent to the other client
     */
    void send(String message);

    /**
     * Initiates file transfer.
     * @param file path to a file to be sent
     */
    void send(Path file);

}
