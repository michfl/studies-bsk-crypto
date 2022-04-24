package pl.edu.pg.eti.ksr.project.network.thread;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;

import java.io.IOException;

@AllArgsConstructor
@Getter
@Setter
public class TcpServerListener implements Runnable {

    private TcpManager manager;

    @Override
    public void run() {
        try {
            manager.setClientSocket(manager.getServerSocket().accept());
            manager.changeStatus(NetworkManager.Status.CONNECTED);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            manager.changeStatus(NetworkManager.Status.READY);
        }
    }
}
