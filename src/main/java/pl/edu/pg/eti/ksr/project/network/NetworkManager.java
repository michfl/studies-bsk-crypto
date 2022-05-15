package pl.edu.pg.eti.ksr.project.network;

import pl.edu.pg.eti.ksr.project.network.data.Frame;

import java.net.SocketTimeoutException;

/**
 * Basic functionality of Network Manager.
 */
public interface NetworkManager {

    enum Status {

        // Manager is ready, no server is running and no connection is established yet.
        READY,

        // Server is started and listening for connections.
        LISTENING,

        // Connection established. Able to exchange data.
        CONNECTED
    }

    int DEFAULT_PORT = 6666;

    /**
     * Gets port that current server is listening on.
     * @return listening port or -1 if no server is running or connection is established.
     */
    int getListeningPort();

    /**
     * Gets port that the connection is established on.
     * @return client port of the connection or -1 if connection is not established.
     */
    int getPort();

    /**
     * Starts server on given port.
     * @param port server port
     * @return true if started successfully, or false if exception thrown or connection already established
     */
    boolean listenOn(int port);

    /**
     * Starts server on default port.
     * @return true if started successfully, or false if exception thrown or connection already established
     */
    default boolean listen() {
        return listenOn(DEFAULT_PORT);
    }

    /**
     * Stops server, if started.
     * If connection is already established, this function will have no effect.
     */
    void stop();

    /**
     * Checks if server is started.
     * @return true if server running
     */
    boolean isListening();

    /**
     * Attempts to connect to server with a given ip and port.
     * If connection is already established, it will be disconnected.
     * If server is listening, it will be stopped by calling stop() function.
     * @param ip of the server to be connected to
     * @param port of the server to be connected to
     * @return true if connection process started successfully
     */
    boolean connect(String ip, int port);

    /**
     * Disconnects from server, if connected.
     * If other client connected, terminates the connection.
     */
    void disconnect();

    /**
     * Checks if any connection is established.
     * @return true if connected to other server, or other client connected to this server
     */
    boolean isConnected();

    /**
     * Sets the timeout on the client socket, which affects receive method.
     * Persists during connections and reconnections.
     * Set to 0 switch of the timeout.
     * Is set to 0 by default.
     * @param millis timeout is milliseconds
     */
    void setReceiveTimeout(int millis);

    /**
     * Sends frame if connected.
     * If sending frame was unsuccessful, disconnects.
     * @param frame frame to be sent
     * @return true if sent successfully, false otherwise
     */
    boolean send(Frame frame);

    /**
     * Receives frame if connected.
     * All data is written to provided frame object.
     * If timeout reached, throws SocketTimeoutException.
     * If other than SocketTimeoutException thrown, disconnects.
     * @param frame object for data to be written to
     * @return true if receive was successful, false otherwise
     */
    boolean receive(Frame frame) throws SocketTimeoutException;

}
