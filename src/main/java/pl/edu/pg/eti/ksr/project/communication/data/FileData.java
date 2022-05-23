package pl.edu.pg.eti.ksr.project.communication.data;

import lombok.*;

/**
 * Message file data.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class FileData {

    /**
     * Original received file name with file extensions.
     */
    String originalFileName;

    /**
     * Current file name.
     * It may be the same as original or changed if name collision occurred.
     */
    String fileName;

    /**
     * Path to the newly received file.
     */
    String filePath;
}
