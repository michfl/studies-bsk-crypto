package pl.edu.pg.eti.ksr.project.communication.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;


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
}
