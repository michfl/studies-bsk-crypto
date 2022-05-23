package pl.edu.pg.eti.ksr.project.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.network.data.Frame;

/**
 * Used for taking encrypted data from the file part queue and sending it to the other client via tcp manager.
 */
@AllArgsConstructor
@Getter
@Setter
public class EncryptedFileSender implements Runnable {

    /**
     * Reference to the communicator object.
     */
    EncryptedTcpCommunicator communicator;

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] data = communicator.filePartQueue.take();
                communicator.tcpManager.send(new Frame(Frame.Type.TRANSFER_DATA, data));
                if (data.length == 0) {
                    communicator.newMessage(Message.Type.FILE_READY, null);
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            communicator.cyphering = false;
        }
    }
}
