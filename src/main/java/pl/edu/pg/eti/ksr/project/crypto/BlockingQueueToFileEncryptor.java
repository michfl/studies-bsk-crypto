package pl.edu.pg.eti.ksr.project.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encrypts data in provided input queue to a file.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BlockingQueueToFileEncryptor implements Runnable {

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
     * Queue with data to perform encryption.
     */
    private BlockingQueue<byte[]> queue;

    /**
     * Path to an output file.
     */
    private Path output;

    /**
     * Flag used for safely stopping thread.
     */
    private AtomicBoolean running;

    @Override
    public void run() {
        CipherOutputStream out;
        try {
            out = new CipherOutputStream(new FileOutputStream(output.toFile()), cipher);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // Get elements from queue and write to output stream ...
    }
}
