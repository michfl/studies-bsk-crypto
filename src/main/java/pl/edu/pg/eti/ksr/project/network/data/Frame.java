package pl.edu.pg.eti.ksr.project.network.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Frame encapsulating all data exchanged between clients.
 */
@NoArgsConstructor
@AllArgsConstructor
public class Frame implements Serializable {

    /**
     * All possible frame types.
     */
    public enum Type {

        // Public key and username exchange
        COMMUNICATION_INIT,

        // Informs about communication stop (disconnection from chat)
        COMMUNICATION_STOP,

        // Symmetric key and symmetric cyphering algorithm info exchange
        SESSION_INIT,

        // Simple text string message exchange
        MESSAGE,

        // File transfer initialization
        TRANSFER_INIT,

        // File data exchange
        TRANSFER_DATA
    }

    /**
     * Frame type.
     */
    public Type frameType;

    /**
     * Data related to this frame.
     */
    public Object data;
}
