package pl.edu.pg.eti.ksr.project.network.data;

import lombok.*;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;

import javax.crypto.spec.IvParameterSpec;
import java.io.Serializable;

/**
 * Used during session initialization.
 * Consists of encrypted symmetric session key and cypher algorithm information.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SessionInfo implements Serializable {

    /**
     * Symmetric session key used in data cyphering.
     */
    byte[] encryptedSessionKey;

    /**
     * IV used in data cyphering.
     */
    byte[] iv;

    /**
     * Transformation used in data cyphering.
     */
    Transformation transformation;

}
