package pl.edu.pg.eti.ksr.project.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encrypts given input file to a provided output queue.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileToBlockingQueueEncryptor implements Runnable {

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
     * Queue to which encrypted data will be inserted.
     * Provided queue should be bounded to prevent keeping all file data in memory.
     */
    private BlockingQueue<byte[]> queue;

    /**
     * Flag used for safely stopping thread.
     */
    private AtomicBoolean running;

    @Override
    public void run() {
        CipherInputStream in;
        try {
            in = new CipherInputStream(new FileInputStream(input.toFile()), cipher);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] cyphered;
        int count;
        try {
            while ((count = in.read(buffer)) > 0 && running.get()) {
                cyphered = new byte[count];
                System.arraycopy(buffer, 0, cyphered, 0, count);
                queue.put(cyphered);
            }
            queue.put(new byte[0]); // stop condition - end of file
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
