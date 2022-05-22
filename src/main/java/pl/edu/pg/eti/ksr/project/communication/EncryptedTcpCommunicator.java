package pl.edu.pg.eti.ksr.project.communication;

import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.crypto.EncryptionManager;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;
import pl.edu.pg.eti.ksr.project.network.data.CommunicationInfo;
import pl.edu.pg.eti.ksr.project.network.data.Frame;
import pl.edu.pg.eti.ksr.project.observer.Observer;
import pl.edu.pg.eti.ksr.project.observer.Subject;

import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Path;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Communication manager.
 *
 * Used for initiating communication, stopping communication, sending and receiving supported formats of messages.
 * Uses tcp communication to perform data exchange and encryption manager for data cyphering.
 * Connection itself should be managed externally.
 * Can modify transformation algorithm of encryption manager depending on the current data exchange needs.
 *
 * All received messages are placed in message queue, as well as published to subscribers.
 * Messages should be removed from the message queue when received and handled by external object.
 *
 * Should be initiated on creation by calling init() to activate the automatic start of receiver thread on connection
 * state change.
 *
 * Should be closed by calling close() on teardown.
 */
@Getter
public class EncryptedTcpCommunicator implements Observer, Subject {

    /**
     * Username of this client.
     */
    @Setter
    String username;

    /**
     * Username of the other client.
     * Will be set after establishing communication.
     */
    String otherUsername;

    /**
     * Public key of this client.
     */
    PublicKey userPublicKey;

    /**
     * Private key of this client.
     */
    PrivateKey userPrivateKey;

    /**
     * Public key of the other client.
     * Will be set after establishing communication.
     */
    PublicKey otherUserPublicKey;

    /**
     * Session key used in symmetric cyphering of data.
     * Will be set after establishing new session.
     */
    Key sessionKey;

    /**
     * Algorithm used in symmetric cyphering of data.
     * Will be set after establishing new session.
     */
    String sessionKeyAlgorithm;

    /**
     * IV used in symmetric cyphering of data.
     * Will be set after establishing new session.
     */
    IvParameterSpec sessionIV;

    /**
     * Incoming messages queue.
     */
    BlockingQueue<Message> messageQueue;

    /**
     * Reference to the tcp manager used for data exchange.
     */
    @Setter
    TcpManager tcpManager;

    /**
     * Reference to the encryption manager used data cyphering.
     */
    @Setter
    EncryptionManager encryptionManager;

    /**
     * Incoming handler running flag used for stopping the thread.
     */
    private final AtomicBoolean running;

    /**
     * Reference to the currently running incoming handler.
     */
    private Thread incomingHandler;

    /**
     * List of all observers subscribing to this object.
     */
    private final Queue<Observer> observers;

    @Override
    public void attach(Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notifyObs(Object o) {
        for (Observer observer : this.observers) {
            observer.update(o);
        }
    }

    /**
     * Internal method for publishing new message arrival.
     * @param type type of the message
     * @param data data related to the message
     * @throws InterruptedException when thread interrupted
     */
    void newMessage(Message.Type type, Object data) throws InterruptedException {
        Message message = new Message(type, new Date(System.currentTimeMillis()), data);
        messageQueue.put(message);
        notifyObs(message);
    }

    /**
     * Used for communicator initialization.
     * After init incoming handler will be started automatically whenever connection is established
     * and stop when connection is lost.
     */
    public void init() {
        tcpManager.attach(this);
        if (tcpManager.getStatus() == NetworkManager.Status.CONNECTED) {
            startIncomingHandler();
        }
    }

    /**
     * Should be called on teardown if was initiated.
     * Detaches communicator object from tcp manager.
     */
    public void close() {
        tcpManager.detach(this);
        stopIncomingHandler();
    }

    /**
     * Starts and stops incoming handler depending on connection state.
     * @param o data related to event
     */
    @Override
    public void update(Object o) {
        NetworkManager.Status status = (NetworkManager.Status) o;

        if (status == NetworkManager.Status.CONNECTED) {
            startIncomingHandler();
        } else {
            stopIncomingHandler();
        }
    }

    /**
     * Starts incoming handler.
     */
    public void startIncomingHandler() {
        if (!running.get()) {
            running.set(true);
            incomingHandler = new Thread(new IncomingHandler(this, running));
            incomingHandler.start();
        }
    }

    /**
     * Stops incoming handler.
     */
    public void stopIncomingHandler() {
        running.set(false);
        if (incomingHandler != null && incomingHandler.isAlive()) incomingHandler.interrupt();
    }

    /**
     * Generates random challenge string.
     * @return random challenge string.
     */
    public static String generateChallenge() {
        int randomInt = new Random().nextInt(9999) + 10000;
        return String.valueOf(randomInt);
    }

    /**
     * Initiates communication by sending username, public key and challenge text to the other client.
     * Make sure that encryption manager is set to an asymmetric algorithm to be used in key exchange.
     * @throws CommunicationException when encryption transformation is not asymmetric
     */
    public void initiateCommunication() throws CommunicationException {
        if (!Objects.equals(encryptionManager.getTransformation().split("/")[0], "RSA")) {
            throw new CommunicationException("Encryption transformation cannot be symmetric.");
        }

        CommunicationInfo info = CommunicationInfo.builder()
                .username(username)
                .userPublicKey(userPublicKey)
                .num(0)
                .asymmetricAlgorithm(encryptionManager.getTransformation())
                .build();
        Frame frame = new Frame(Frame.Type.COMMUNICATION_INIT, info);

        tcpManager.send(frame);
    }

    public EncryptedTcpCommunicator(String username, PublicKey userPublicKey, PrivateKey userPrivateKey,
                                    TcpManager tcpManager, EncryptionManager encryptionManager) {
        this.username = username;
        this.userPublicKey = userPublicKey;
        this.userPrivateKey = userPrivateKey;
        this.tcpManager = tcpManager;
        this.encryptionManager = encryptionManager;
        this.running = new AtomicBoolean(false);
        this.messageQueue = new LinkedBlockingDeque<>();
        this.observers = new ConcurrentLinkedQueue<>();
    }
}
