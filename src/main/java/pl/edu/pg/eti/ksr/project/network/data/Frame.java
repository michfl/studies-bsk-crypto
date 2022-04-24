package pl.edu.pg.eti.ksr.project.network.data;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
public class Frame implements Serializable {

    public enum Type {
        CONFIGURATION,
        DATA
    }

    public Type frameType;

    public Object data;
}
