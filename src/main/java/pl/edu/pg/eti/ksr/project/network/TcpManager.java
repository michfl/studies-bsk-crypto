package pl.edu.pg.eti.ksr.project.network;

import lombok.Getter;
import pl.edu.pg.eti.ksr.project.network.data.Frame;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpManager implements NetworkManager {

    ServerSocket serverSocket;

    Socket clientSocket;

    ObjectInputStream in;

    ObjectOutputStream out;

    @Getter
    private Status status;

    void changeStatus(Status status) { // add change status observer
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
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
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
        if (status != Status.CONNECTED) return;

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
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        in = null;
        out = null;
        clientSocket = null;
        serverSocket = null;
        changeStatus(Status.READY);
    }

    @Override
    public boolean isConnected() {
        return status == Status.CONNECTED;
    }

    @Override
    public boolean send(Frame frame) {
        if (status != Status.CONNECTED) return false;

        try {
            out.writeObject(frame);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
            return false;
        }

        return true;
    }

    @Override
    public boolean receive(Frame frame) {
        if (status != Status.CONNECTED) return false;

        try {
            Frame received = (Frame) in.readObject();
            frame.frameType = received.frameType;
            frame.data = received.data;
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public TcpManager() {
        this.status = Status.READY;
    }
}
