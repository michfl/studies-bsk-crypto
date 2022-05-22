package pl.edu.pg.eti.ksr.project.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * New connection listener thread.
 */
@AllArgsConstructor
@Getter
@Setter
public class TcpServerListener implements Runnable {

    /**
     * Reference to the tcp manager object.
     */
    private TcpManager manager;

    @Override
    public void run() {
        try {
            manager.clientSocket = manager.serverSocket.accept();
            manager.out = new ObjectOutputStream(manager.clientSocket.getOutputStream());
            manager.in = new ObjectInputStream(manager.clientSocket.getInputStream());
            manager.changeStatus(NetworkManager.Status.CONNECTED);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            manager.changeStatus(NetworkManager.Status.READY);
        }
    }
}
