package pl.edu.pg.eti.ksr.project.communication;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

/**
 * Interface for communication between two clients.
 * Does not manage the connection between clients.
 * Used for initiating, checking and  communication and sending supported formats of messages.
 */
public interface CommunicationManager {

    void initiateSession();

    boolean checkSessionState();

    void stopSession();

    void send(String message);

    void send(Path file);

    void send(BlockingQueue<byte[]> queue);

}
