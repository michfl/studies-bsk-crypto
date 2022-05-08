package pl.edu.pg.eti.ksr.project.crypto.file;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;

/**
 * Class used for managing attributes associated with files.
 */
public class FileInfo {

    /**
     * Extracts file attributes and returns them as a JSON object.
     * @param file path to the file
     * @return JSON object containing extracted attributes
     * @throws IOException problem with reading attributes from the file
     */
    public static JSONObject getFileAttrAsJSON(Path file) throws IOException {

        JSONObject json = new JSONObject();
        BasicFileAttributes basicView = Files.readAttributes(file, BasicFileAttributes.class);
        FileOwnerAttributeView ownerView = Files.getFileAttributeView(file, FileOwnerAttributeView.class);

        // Full file name with file type
        json.put("fileName", file.getFileName().toString());

        // File creation time in millis
        json.put("creationTime", basicView.creationTime().toMillis());

        // File last modified time in millis
        json.put("lastModifiedTime", basicView.lastModifiedTime().toMillis());

        // File owner
        json.put("owner", ownerView.getOwner().getName());

        // Extract more data here if necessary

        return json;
    }

//    public static Path setFileAttrFromJSON(Path file, JSONObject json) throws IOException {
//
//        Path target = Files.move(file, file.resolveSibling((String) json.get("fileName")));
//
//        Files.set
//
//        return target;
//    }

}
