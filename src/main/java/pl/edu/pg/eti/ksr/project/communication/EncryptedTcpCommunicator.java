package pl.edu.pg.eti.ksr.project.communication;

import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.communication.data.FileData;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.crypto.EncryptionManager;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;
import pl.edu.pg.eti.ksr.project.network.data.CommunicationInfo;
import pl.edu.pg.eti.ksr.project.network.data.FileInfo;
import pl.edu.pg.eti.ksr.project.network.data.Frame;
import pl.edu.pg.eti.ksr.project.network.data.SessionInfo;
import pl.edu.pg.eti.ksr.project.observer.Observer;
import pl.edu.pg.eti.ksr.project.observer.Subject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Date;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
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
     * Path to a directory where received files will be stored.
     * Should end with '/', e.g. ".../directory/"
     */
    @Setter
    String savedFilesPath;

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
     * Transformation used in asymmetric key pair generation.
     */
    Transformation asymmetricTransformation;

    /**
     * True if communication is established.
     */
    boolean communicationEstablished;

    /**
     * Session key used in symmetric cyphering of data.
     * Will be set after establishing new session.
     */
    Key sessionKey;

    /**
     * Transformation used in symmetric cyphering of data.
     * Will be set after establishing new session.
     */
    Transformation symmetricTransformation;

    /**
     * IV used in symmetric cyphering of data.
     * Will be set after establishing new session.
     */
    IvParameterSpec sessionIV;

    /**
     * True if session is established.
     */
    boolean sessionEstablished;

    /**
     * Incoming messages queue.
     */
    BlockingQueue<Message> messageQueue;

    /**
     * Queue consisting received file data for decryption.
     */
    BlockingQueue<byte[]> filePartQueue;

    /**
     * Object consisting latest received file data.
     * Will be set after initiation of file transfer.
     */
    FileData latestFileData;

    /**
     * True if currently in process of file decryption or encryption.
     */
    boolean cyphering;

    /**
     * Reference to the encrypted file sender thread.
     */
    private Thread fileSender;

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
     * Stop current cyphering work.
     */
    public void stopCyphering() {
        encryptionManager.stopCurrentWork();
        if (fileSender != null && fileSender.isAlive()) fileSender.interrupt();
        filePartQueue.clear();
        cyphering = false;
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
        stopCyphering();
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
            stopCyphering();
            communicationEstablished = false;
            sessionEstablished = false;
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
     * Make sure that provided algorithm is an asymmetric algorithm to be used in key exchange.
     * @throws CommunicationException when called during ongoing cyphering operation
     */
    public void initiateCommunication() throws CommunicationException {
        if (cyphering) {
            throw new CommunicationException("Cannot initiate session during ongoing cyphering process.");
        }

        CommunicationInfo info = CommunicationInfo.builder()
                .username(username)
                .userPublicKey(userPublicKey)
                .num(0)
                .build();
        Frame frame = new Frame(Frame.Type.COMMUNICATION_INIT, info);

        tcpManager.send(frame);
    }

    /**
     * Sends communication-stop message to announce to the other client that current communication is no longer active.
     */
    public void stopCommunication() {
        communicationEstablished = false;
        sessionEstablished = false;

        Frame frame = new Frame(Frame.Type.COMMUNICATION_STOP, null);
        tcpManager.send(frame);
    }

    /**
     * Initiates session by sending session info to the other client.
     * Before sending generates session key and IV based on provided transformation.
     * One communication can consist of multiple sessions with different parameters.
     * @param transformation algorithm to be used in symmetric cyphering
     * @throws CommunicationException when called during ongoing cyphering operation
     * @throws NoSuchAlgorithmException provided incorrect algorithm
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     * @throws InvalidKeyException problem with key
     * @throws NoSuchPaddingException problem with padding
     */
    public void initiateSession(Transformation transformation) throws CommunicationException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchPaddingException {
        if (cyphering) {
            throw new CommunicationException("Cannot initiate session during ongoing cyphering process.");
        }
        if (!communicationEstablished) return;

        if (!Objects.equals(encryptionManager.getTransformation(), asymmetricTransformation.getText())) {
            encryptionManager.setTransformation(asymmetricTransformation.getText());
        }

        sessionKey = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        sessionIV = EncryptionManager.generateIv(transformation.getBlockSize());
        symmetricTransformation = transformation;

        SessionInfo info = SessionInfo.builder()
                .encryptedSessionKey(encryptionManager.encrypt(sessionKey, otherUserPublicKey))
                .iv(sessionIV.getIV())
                .transformation(symmetricTransformation)
                .build();

        Frame frame = new Frame(Frame.Type.SESSION_INIT, info);

        tcpManager.send(frame);

        sessionEstablished = true;
    }

    /**
     * Sends text message to the other client.
     * Uses established session info to perform text encryption.
     * If no session has been established it will take no effect.
     * @param message text to be sent
     * @throws InvalidAlgorithmParameterException wrong algorithm parameters
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     * @throws InvalidKeyException problem with key
     * @throws NoSuchPaddingException problem with padding
     * @throws NoSuchAlgorithmException provided incorrect algorithm
     */
    public void send(String message) throws InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (!sessionEstablished) return;

        if (!Objects.equals(encryptionManager.getTransformation(), symmetricTransformation.getText())) {
            encryptionManager.setTransformation(symmetricTransformation.getText());
        }

        byte[] encMessage;

        if (Objects.equals(symmetricTransformation.getMode(), "CBC")) {
            encMessage = encryptionManager.encrypt(message, sessionKey, sessionIV);
        } else {
            encMessage = encryptionManager.encrypt(message, sessionKey);
        }

        Frame frame = new Frame(Frame.Type.MESSAGE, encMessage);

        tcpManager.send(frame);
    }

    /**
     * Initiates file transfer and starts file encryption and sending threads.
     * Used for sending files to the other client.
     *
     * NOTE: Before transfer process completes (cyphering flag set to false) or is cancelled (stopCyphering)
     *       it is not possible to initiate new session or change cyphering options.
     *       Changing cyphering transformation during transfer process will result in cyphering exception.
     *
     * @param pathToFile file to be encrypted and send
     * @throws CommunicationException when trying to invoke before previous transfer has ended
     * @throws IOException provided incorrect file path
     * @throws NoSuchPaddingException problem with padding
     * @throws NoSuchAlgorithmException problem with chosen transformation
     * @throws InvalidAlgorithmParameterException wrong algorithm parameters
     * @throws InvalidKeyException problem with key
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     */
    public void send(Path pathToFile) throws CommunicationException, IOException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        if (cyphering) {
            throw new CommunicationException("Cannot initiate another transfer during ongoing cyphering process.");
        }
        if (!sessionEstablished) return;

        if (!Objects.equals(encryptionManager.getTransformation(), symmetricTransformation.getText())) {
            encryptionManager.setTransformation(symmetricTransformation.getText());
        }

        long fileSize = Files.size(pathToFile);
        String fileName = pathToFile.getFileName().toString();

        FileInfo fileInfo;
        if (Objects.equals(symmetricTransformation.getMode(), "CBC")) {
            fileInfo = new FileInfo(encryptionManager.encrypt(fileName, sessionKey, sessionIV),
                    encryptionManager.encrypt(String.valueOf(fileSize), sessionKey, sessionIV));
        } else {
            fileInfo = new FileInfo(encryptionManager.encrypt(fileName, sessionKey),
                    encryptionManager.encrypt(String.valueOf(fileSize), sessionKey));
        }

        tcpManager.send(new Frame(Frame.Type.TRANSFER_INIT, fileInfo));

        cyphering = true;
        filePartQueue.clear();

        cyphering = true;
        if (Objects.equals(symmetricTransformation.getMode(), "CBC")) {
            encryptionManager.encrypt(pathToFile, filePartQueue, sessionKey, sessionIV, fileSize);
        } else {
            encryptionManager.encrypt(pathToFile, filePartQueue, sessionKey, fileSize);
        }

        fileSender = new Thread(new EncryptedFileSender(this));
        fileSender.start();
    }

    public EncryptedTcpCommunicator(String savedFilesPath, String username, PublicKey userPublicKey,
                                    PrivateKey userPrivateKey, Transformation asymmetricTransformation,
                                    TcpManager tcpManager, EncryptionManager encryptionManager) {
        this.savedFilesPath = savedFilesPath;
        this.username = username;
        this.userPublicKey = userPublicKey;
        this.userPrivateKey = userPrivateKey;
        this.asymmetricTransformation = asymmetricTransformation;
        this.tcpManager = tcpManager;
        this.encryptionManager = encryptionManager;
        this.running = new AtomicBoolean(false);
        this.messageQueue = new LinkedBlockingDeque<>();
        this.filePartQueue = new LinkedBlockingDeque<>();
        this.observers = new ConcurrentLinkedQueue<>();
        this.communicationEstablished = false;
        this.sessionEstablished = false;
        this.cyphering = false;
    }
}
