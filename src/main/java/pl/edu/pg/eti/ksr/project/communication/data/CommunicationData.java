package pl.edu.pg.eti.ksr.project.communication.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.PublicKey;

/**
 * Message communication data.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CommunicationData {

    /**
     * Other user username.
     */
    String username;

    /**
     * Other user public key.
     */
    PublicKey userPublicKey;
}
