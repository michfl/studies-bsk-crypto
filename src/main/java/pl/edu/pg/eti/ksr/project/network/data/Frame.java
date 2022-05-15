package pl.edu.pg.eti.ksr.project.network.data;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
public class Frame implements Serializable {

    public enum Type {

        // Public key and username exchange
        SESSION_INIT,

        // Connection check
        SESSION_CHECK,

        // Symmetric key and symmetric cyphering algorithm info exchange
        CYPHER_INIT,

        // Simple text string message exchange
        MESSAGE,

        // File transfer initialization
        TRANSFER_INIT,

        // File data exchange
        TRANSFER_DATA
    }

    public Type frameType;

    public Object data;
}
