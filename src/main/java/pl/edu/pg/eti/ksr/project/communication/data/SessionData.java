package pl.edu.pg.eti.ksr.project.communication.data;

import lombok.*;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;

/**
 * Message session data.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class SessionData {

    /**
     * Transformation to be used in symmetric data exchange.
     */
    Transformation transformation;
}
