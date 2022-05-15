package pl.edu.pg.eti.ksr.project.network.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.crypto.spec.IvParameterSpec;
import java.io.Serializable;
import java.security.PublicKey;

/**
 * Session data object.
 * Consists all session information.
 * Used during session initialization.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SessionInfo implements Serializable {

    /**
     * Username to be associated with incoming messages.
     */
    String username;

    /**
     * User public key.
     */
    PublicKey userPublicKey;

    /**
     * Symmetric session key to be used in later data encryption.
     */
    byte[] encryptedSessionKey;

    /**
     * IV to be used in later data encryption.
     */
    IvParameterSpec iv;

    /**
     * Algorithm used
     */
    String algorithm;

}
