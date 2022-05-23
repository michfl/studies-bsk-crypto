package pl.edu.pg.eti.ksr.project.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
        CipherOutputStream out;
        long total = 0;

        try {
            out = new CipherOutputStream(new FileOutputStream(output.toFile()), cipher);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        byte[] buffer;
        try {
            while (running.get()) {
                buffer = queue.take();
                if (buffer.length == 0) break; // stop condition met

                total = Math.min(total + buffer.length, fileSize);
                manager.publishEncryptionState((double)total / fileSize);

                out.write(buffer, 0, buffer.length);
            }
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
