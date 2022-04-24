package pl.edu.pg.eti.ksr.project.network;

import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.network.thread.TcpServerListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpManager implements NetworkManager {

    @Getter
    @Setter
    private ServerSocket serverSocket;

    @Getter
    @Setter
    private Socket clientSocket;

    @Getter
    private Status status;

    public void changeStatus(Status status) { // add change status observer
        if (status != this.status) {

            this.status = status;
        }
    }

    @Override
    public int getPort() {
        return status == Status.CONNECTED ? clientSocket.getPort() : -1;
    }

    @Override
    public int getListeningPort() {
        return status == Status.LISTENING ? serverSocket.getLocalPort() : -1;
    }

    @Override
    public boolean listenOn(int port) {
        if (status == Status.CONNECTED) return false;
        if (status == Status.LISTENING) stop();

        try {
            serverSocket = new ServerSocket(port);

            new Thread(new TcpServerListener(this)).start();

            changeStatus(Status.LISTENING);

        } catch (IOException e) {
            e.printStackTrace();
            serverSocket = null;
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        if (serverSocket != null && status == Status.LISTENING) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                serverSocket = null;
            }
        }
    }

    @Override
    public boolean isListening() {
        return status == Status.LISTENING;
    }

    @Override
    public boolean connect(String ip, int port) {
        if (status == Status.LISTENING) stop();
        if (status == Status.CONNECTED) disconnect();

        try {
            clientSocket = new Socket(ip, port);
            changeStatus(Status.CONNECTED);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            clientSocket = null;
            changeStatus(Status.READY);
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (status == Status.CONNECTED) {
            try {
                if (serverSocket != null) serverSocket.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientSocket = null;
                serverSocket = null;
                changeStatus(Status.READY);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return status == Status.CONNECTED;
    }

    public TcpManager() {
        this.status = Status.READY;
    }
}
