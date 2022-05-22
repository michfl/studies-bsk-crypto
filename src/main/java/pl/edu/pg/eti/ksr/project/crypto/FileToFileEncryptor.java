package pl.edu.pg.eti.ksr.project.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encrypts given input file to a provided output file.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileToFileEncryptor implements Runnable {

    /**
     * Sets buffer size for ciphering.
     * Prevents keeping all file data in memory.
     */
    public static final int BUFFER_SIZE = 8192;

    /**
     * Initialized cipher object to be used in ciphering.
     */
    private Cipher cipher;

    /**
     * Path to a file to be encrypted.
     */
    private Path input;

    /**
     * Path to a file that will be the encrypted input file.
     */
    private Path output;

    /**
     * Flag used for safely stopping thread.
     */
    private AtomicBoolean running;

    /**
     * Size of a file in bytes.
     */
    private long fileSize;

    /**
     * Reference to the calling class.
     * Used to publish encryption state.
     */
    private EncryptionManager manager;

    @Override
    public void run() {
        FileInputStream in;
        CipherOutputStream out;
        long total = 0;

        try {
            in = new FileInputStream(input.toFile());
            out = new CipherOutputStream(new FileOutputStream(output.toFile()), cipher);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        int count;
        try {
            while ((count = in.read(buffer)) > 0 && running.get()) {
                total = Math.min(total + count, fileSize);
                manager.publishEncryptionState((double)total / fileSize);

                out.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
