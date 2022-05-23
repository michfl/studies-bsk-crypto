package pl.edu.pg.eti.ksr.project.communication;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.edu.pg.eti.ksr.project.communication.data.FileData;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.crypto.EncryptionManager;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;
import pl.edu.pg.eti.ksr.project.observer.Observer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class FullCommunicationScenarioTest {

    private TcpManager tcpManager1;
    private TcpManager tcpManager2;

    private EncryptionManager encryptionManager1;
    private EncryptionManager encryptionManager2;
    private Transformation transformation = Transformation.RSA_ECB_PKCS1Padding;
    private KeyPair keyPair1;
    private KeyPair keyPair2;

    private final String savedFilesPath = "./src/test/resources/";

    private final Path sourceFile = Path.of("./src/test/resources/test.txt");
    private final Path targetDecryptedFile = Path.of("./src/test/resources/test_1.txt");

    private EncryptedTcpCommunicator encryptedTcpCommunicator1;
    private EncryptedTcpCommunicator encryptedTcpCommunicator2;
    private final String username1 = "test_username_1";
    private final String username2 = "test_username_2";

    private static class DummyTcpManager1Observer implements Observer {
        @Override
        public void update(Object o) {
            NetworkManager.Status status = (NetworkManager.Status) o;
            System.out.println("TCP Manager 1 STATUS: " + status.toString());
        }
    }

    private static class DummyTcpManager2Observer implements Observer {
        @Override
        public void update(Object o) {
            NetworkManager.Status status = (NetworkManager.Status) o;
            System.out.println("TCP Manager 2 STATUS: " + status.toString());
        }
    }

    private static class DummyEncryptionManager1Observer implements Observer {
        @Override
        public void update(Object o) {
            double progress = (double) o;
            System.out.println("Encryption Manager 1 PROGRESS: " + progress);
        }
    }

    private static class DummyEncryptionManager2Observer implements Observer {
        @Override
        public void update(Object o) {
            double progress = (double) o;
            System.out.println("Encryption Manager 2 PROGRESS: " + progress);
        }
    }

    private static class DummyEncryptedTcpCommunicator1Observer implements Observer {
        @Override
        public void update(Object o) {
            Message message = (Message) o;
            System.out.println("Encrypted TCP Communicator 1 MESSAGE: " + message.toString());
        }
    }

    private static class DummyEncryptedTcpCommunicator2Observer implements Observer {
        @Override
        public void update(Object o) {
            Message message = (Message) o;
            System.out.println("Encrypted TCP Communicator 2 MESSAGE: " + message.toString());
        }
    }

    private Callable<Boolean> manager1HasStatusListening() {
        return () -> tcpManager1.getStatus() == NetworkManager.Status.LISTENING;
    }

    private Callable<Boolean> manager2HasStatusConnected() {
        return () -> tcpManager2.getStatus() == NetworkManager.Status.CONNECTED;
    }

    private Callable<Boolean> communicator1IncomingHandlerIsAlive() {
        return () -> encryptedTcpCommunicator1.getIncomingHandler().isAlive();
    }

    private Callable<Boolean> communicator2IncomingHandlerIsAlive() {
        return () -> encryptedTcpCommunicator2.getIncomingHandler().isAlive();
    }

    private Callable<Boolean> communicator2ReceivedCommInfo1() {
        return () -> Objects.equals(encryptedTcpCommunicator2.getOtherUsername(), username1) &&
                encryptedTcpCommunicator2.getOtherUserPublicKey().equals(keyPair1.getPublic());
    }

    private Callable<Boolean> communicator1ReceivedCommInfo2() {
        return () -> Objects.equals(encryptedTcpCommunicator1.getOtherUsername(), username2) &&
                encryptedTcpCommunicator1.getOtherUserPublicKey().equals(keyPair2.getPublic());
    }

    private Callable<Boolean> communicator1SessionEstablished() {
        return () -> encryptedTcpCommunicator1.sessionEstablished;
    }

    private Callable<Boolean> communicator2SessionEstablished() {
        return () -> encryptedTcpCommunicator2.sessionEstablished;
    }

    @Before
    public void init() throws NoSuchPaddingException, NoSuchAlgorithmException {

        // Creating tcp managers for both clients
        tcpManager1 = new TcpManager();
        tcpManager2 = new TcpManager();

        // Attaching dummy observers for observation purposes
        tcpManager1.attach(new DummyTcpManager1Observer());
        tcpManager2.attach(new DummyTcpManager2Observer());

        // Creating encryption managers for both clients
        encryptionManager1 = new EncryptionManager(transformation.getText());
        encryptionManager2 = new EncryptionManager(transformation.getText());

        // Attaching dummy observers for observation purposes
        encryptionManager1.attach(new DummyEncryptionManager1Observer());
        encryptionManager2.attach(new DummyEncryptionManager2Observer());

        // Generating public private key pairs for both clients for test scenario purposes
        keyPair1 = EncryptionManager.generateKeyPair(transformation.getKeySize(),
                transformation.getAlgorithm());
        keyPair2 = EncryptionManager.generateKeyPair(transformation.getKeySize(),
                transformation.getAlgorithm());

        // Creating encrypted tcp communicators representing both clients
        encryptedTcpCommunicator1 = new EncryptedTcpCommunicator(savedFilesPath, username1, keyPair1.getPublic(),
                keyPair1.getPrivate(), transformation, tcpManager1, encryptionManager1);
        encryptedTcpCommunicator2 = new EncryptedTcpCommunicator(savedFilesPath, username2, keyPair2.getPublic(),
                keyPair2.getPrivate(), transformation, tcpManager2, encryptionManager2);

        // Attaching dummy observers for observation purposes
        encryptedTcpCommunicator1.attach(new DummyEncryptedTcpCommunicator1Observer());
        encryptedTcpCommunicator2.attach(new DummyEncryptedTcpCommunicator2Observer());

        // Initializing communicators for both clients
        encryptedTcpCommunicator1.init();
        encryptedTcpCommunicator2.init();
    }

    @After
    public void teardown() {

        // Stopping current work if active and disconnecting tcp managers for client 1
        tcpManager1.stop();
        tcpManager1.disconnect();

        // Stopping current work if active and disconnecting tcp managers for client 2
        tcpManager2.stop();
        tcpManager2.disconnect();

        // Closing both clients communicators
        encryptedTcpCommunicator1.close();
        encryptedTcpCommunicator2.close();

        // Some test cleanup
        File trgDecrypted = targetDecryptedFile.toFile();
        if (trgDecrypted.exists()) trgDecrypted.delete();
    }

    @Test
    public void testScenario1() throws CommunicationException, IllegalBlockSizeException, NoSuchPaddingException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
            InterruptedException, IOException {

        // Client 1 starts its tcp listener to enable connection for client 2
        encryptedTcpCommunicator1.getTcpManager().listen();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        // Client 2 tries to connect to client one on localhost and default port
        encryptedTcpCommunicator2.getTcpManager().connect("localhost", NetworkManager.DEFAULT_PORT);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsAlive());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2IncomingHandlerIsAlive());

        // At this point socket connection between client 1 and 2 should be established

        // Client 1 initiates communication with client 2
        encryptedTcpCommunicator1.initiateCommunication();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1ReceivedCommInfo2());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2ReceivedCommInfo1());

        // At this point both clients should know username and public key of the other party

        // Client 1 initiates new session with client 2 using AES CBC transformation
        encryptedTcpCommunicator1.initiateSession(Transformation.AES_CBC_PKCS5Padding);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1SessionEstablished());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2SessionEstablished());
        Assert.assertEquals(encryptedTcpCommunicator1.getSessionKey(), encryptedTcpCommunicator2.getSessionKey());
        Assert.assertArrayEquals(encryptedTcpCommunicator1.getSessionIV().getIV(),
                encryptedTcpCommunicator2.getSessionIV().getIV());

        // At this point both clients should have the same session key and session IV

        // Clearing message queues of both clients, only for test purposes
        // Messages should be always taken from the queues upon arrival by interested third party
        encryptedTcpCommunicator1.getMessageQueue().clear();
        encryptedTcpCommunicator2.getMessageQueue().clear();

        // Client 1 sends to client 2 text message, which will be encrypted and decrypted using exchanged session key
        String testText1 = "test message";
        encryptedTcpCommunicator1.send(testText1);

        // Client 2 receives message sent by client 1 and takes it from his message queue
        Message testMessage1Received = encryptedTcpCommunicator2.getMessageQueue().poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(testMessage1Received);
        Assert.assertEquals(Message.Type.MESSAGE, testMessage1Received.messageType);

        // Sent and received message should be equal
        Assert.assertEquals(testText1, testMessage1Received.data);

        // Client 2 initiates new session with client 1 using AES ECB transformation
        encryptedTcpCommunicator2.initiateSession(Transformation.AES_ECB_PKCS5Padding);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1SessionEstablished());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2SessionEstablished());
        Assert.assertEquals(encryptedTcpCommunicator1.getSessionKey(), encryptedTcpCommunicator2.getSessionKey());
        Assert.assertArrayEquals(encryptedTcpCommunicator1.getSessionIV().getIV(),
                encryptedTcpCommunicator2.getSessionIV().getIV());

        // At this point both clients should have the same new session key and session IV

        // Clearing message queues of both clients, only for test purposes
        // Messages should be always taken from the queues upon arrival by interested third party
        encryptedTcpCommunicator1.getMessageQueue().clear();
        encryptedTcpCommunicator2.getMessageQueue().clear();

        // Client 2 sends to client 1 a .txt file, which will be encrypted and decrypted using exchanged session key
        // During file transfer both clients should not change their cyphering settings, which means that:
        // - they cannot initiate a new session
        // - they cannot initiate a new communication
        // - they cannot manually change transformation settings in their encryption managers
        // or file transfer will not complete successfully
        encryptedTcpCommunicator2.send(sourceFile);

        // Client 1 gets informed about incoming file transfer and its parameters
        Message messageFile = encryptedTcpCommunicator1.getMessageQueue().poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(messageFile);
        Assert.assertEquals(Message.Type.FILE, messageFile.messageType);

        // At this point file transfer between two clients is happening in the background

        // Client's 1 communicator places a new message to the queue informing,
        // that a file transfer has been completed and file is ready
        Message messageFileReady1 = encryptedTcpCommunicator1.getMessageQueue().poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(messageFileReady1);
        Assert.assertEquals(Message.Type.FILE_READY, messageFileReady1.messageType);

        // Client's 2 communicator places a new message to the queue informing,
        // that a file transfer has been completed and file has been successfully sent to the other client
        Message messageFileReady2 = encryptedTcpCommunicator2.getMessageQueue().poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(messageFileReady2);
        Assert.assertEquals(Message.Type.FILE_READY, messageFileReady2.messageType);

        // Sent and received file should be equal
        Assert.assertTrue(new File(((FileData)messageFile.data).getFilePath()).exists());
        long result = Files.mismatch(sourceFile, targetDecryptedFile);
        Assert.assertEquals(-1L, result);

        // Client 1 sends a communication stop message indicating that he is going to close his connection
        encryptedTcpCommunicator1.stopCommunication();

        // Client 2 receives communication stop message from client 1 and interprets it as connection closed
        Message messageStopCommunication = encryptedTcpCommunicator2.getMessageQueue().poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(messageStopCommunication);
        Assert.assertEquals(Message.Type.COMMUNICATION_STOP, messageStopCommunication.messageType);

        // At this point both clients can gracefully close their connections
    }
}
