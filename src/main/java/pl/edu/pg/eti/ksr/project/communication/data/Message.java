package pl.edu.pg.eti.ksr.project.communication.data;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.security.Timestamp;
import java.util.Date;

/**
 * Messages received by the communicator.
 * Messages are summaries of completed data exchange sequences.
 */
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    /**
     * Possible types of the message.
     */
    public enum Type {

        // New communication established
        COMMUNICATION,

        // Other client closed communication
        COMMUNICATION_STOP,

        // New session established
        SESSION,

        // New text message
        MESSAGE,

        // New file incoming
        FILE,

        // New file ready
        FILE_READY
    }

    /**
     * Type of the message.
     */
    public Type messageType;

    /**
     * Timestamp of message occurrence.
     */
    public Date timestamp;

    /**
     * Data related to the message.
     */
    public Object data;

}
