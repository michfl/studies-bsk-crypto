package pl.edu.pg.eti.ksr.project.network.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * File info object.
 * Consists all information associated with file.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileInfo implements Serializable {

    /**
     * Original file name.
     */
    String fileName;

    /**
     * Original file size.
     */
    long fileSize;
}
