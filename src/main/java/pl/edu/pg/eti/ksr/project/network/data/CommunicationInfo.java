package pl.edu.pg.eti.ksr.project.network.data;

import lombok.*;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;

import javax.crypto.spec.IvParameterSpec;
import java.io.Serializable;
import java.security.PublicKey;

/**
 * Communication data object.
 * Consists all information needed to establish communication.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CommunicationInfo implements Serializable {

    /**
     * Username to be associated with incoming messages.
     */
    String username;

    /**
     * User public key.
     */
    PublicKey userPublicKey;

    /**
     * Used for identifying verification step.
     */
    int num;

    /**
     * Plain text to be encrypted using other client's private key.
     * Used for verification.
     */
    String challenge;

    /**
     * Cypher text consisting challenge encrypted by other client with his private key.
     * Used for verification.
     */
    byte[] challengeResponse;
}
