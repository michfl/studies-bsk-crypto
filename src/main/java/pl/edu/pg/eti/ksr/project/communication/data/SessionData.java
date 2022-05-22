package pl.edu.pg.eti.ksr.project.communication.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;

/**
 * Message session data.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SessionData {

    /**
     * Transformation to be used in symmetric data exchange.
     */
    Transformation transformation;
}
