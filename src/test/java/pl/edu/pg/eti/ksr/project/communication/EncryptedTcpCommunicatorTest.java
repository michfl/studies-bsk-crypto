package pl.edu.pg.eti.ksr.project.communication;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.crypto.EncryptionManager;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;
import pl.edu.pg.eti.ksr.project.network.data.CommunicationInfo;
import pl.edu.pg.eti.ksr.project.network.data.Frame;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class EncryptedTcpCommunicatorTest {

    private TcpManager tcpManager1;
    private TcpManager tcpManager2;

    private EncryptionManager encryptionManager1;
    private EncryptionManager encryptionManager2;
    private Transformation transformation = Transformation.AES_CBC_PKCS5Padding;
    private KeyPair keyPair1;
    private KeyPair keyPair2;


    private EncryptedTcpCommunicator encryptedTcpCommunicator1;
    private EncryptedTcpCommunicator encryptedTcpCommunicator2;
    private final String username1 = "test_username_1";
    private final String username2 = "test_username_2";

    @Before
    public void init() throws NoSuchPaddingException, NoSuchAlgorithmException {
        tcpManager1 = new TcpManager();
        tcpManager2 = new TcpManager();

        encryptionManager1 = new EncryptionManager(transformation.getText());
        encryptionManager2 = new EncryptionManager(transformation.getText());

        keyPair1 = EncryptionManager.generateKeyPair(Transformation.RSA_ECB_PKCS1Padding.getKeySize(),
                Transformation.RSA_ECB_PKCS1Padding.getAlgorithm());
        keyPair2 = EncryptionManager.generateKeyPair(Transformation.RSA_ECB_PKCS1Padding.getKeySize(),
                Transformation.RSA_ECB_PKCS1Padding.getAlgorithm());

        encryptedTcpCommunicator1 = new EncryptedTcpCommunicator(username1, keyPair1.getPublic(), keyPair1.getPrivate(),
                Transformation.RSA_ECB_PKCS1Padding, tcpManager1, encryptionManager1);
        encryptedTcpCommunicator2 = new EncryptedTcpCommunicator(username2, keyPair2.getPublic(), keyPair2.getPrivate(),
                Transformation.RSA_ECB_PKCS1Padding, tcpManager2, encryptionManager2);
    }

    @After
    public void teardown() {
        tcpManager1.stop();
        tcpManager1.disconnect();

        tcpManager2.stop();
        tcpManager2.disconnect();

        encryptedTcpCommunicator1.close();
        encryptedTcpCommunicator2.close();
    }

    private Callable<Boolean> communicator1IncomingHandlerIsAlive() {
        return () -> encryptedTcpCommunicator1.getIncomingHandler().isAlive();
    }

    private Callable<Boolean> communicator1IncomingHandlerIsNotAlive() {
        return () -> !encryptedTcpCommunicator1.getIncomingHandler().isAlive();
    }

    private Callable<Boolean> manager1HasStatusListening() {
        return () -> tcpManager1.getStatus() == NetworkManager.Status.LISTENING;
    }

    private Callable<Boolean> manager2HasStatusConnected() {
        return () -> tcpManager2.getStatus() == NetworkManager.Status.CONNECTED;
    }

    @Test
    public void Should_IncomingHandlerThreadBeStopped_When_Disconnected() {
        encryptedTcpCommunicator1.getTcpManager().listen();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        encryptedTcpCommunicator2.getTcpManager().connect("localhost", NetworkManager.DEFAULT_PORT);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());

        encryptedTcpCommunicator1.startIncomingHandler();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsAlive());

        encryptedTcpCommunicator1.getTcpManager().disconnect();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsNotAlive());
    }

    @Test
    public void Should_IncomingHandlerStartAutomatically_When_CommunicatorInitiated() {
        encryptedTcpCommunicator1.init();

        encryptedTcpCommunicator1.getTcpManager().listen();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        encryptedTcpCommunicator2.getTcpManager().connect("localhost", NetworkManager.DEFAULT_PORT);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsAlive());
    }

    private Callable<Boolean> communicator2IncomingHandlerIsAlive() {
        return () -> encryptedTcpCommunicator2.getIncomingHandler().isAlive();
    }

    private Callable<Boolean> communicator1ReceivedCommInfo2() {
        return () -> Objects.equals(encryptedTcpCommunicator1.getOtherUsername(), username2) &&
                encryptedTcpCommunicator1.getOtherUserPublicKey().equals(keyPair2.getPublic());
    }

    private Callable<Boolean> communicator2ReceivedCommInfo1() {
        return () -> Objects.equals(encryptedTcpCommunicator2.getOtherUsername(), username1) &&
                encryptedTcpCommunicator2.getOtherUserPublicKey().equals(keyPair1.getPublic());
    }

    @Test
    public void Should_ProperlyExchangeUsernamesAndPublicKeys_When_InitiateCommunicationMethodCalled()
            throws CommunicationException {
        encryptedTcpCommunicator1.init();
        encryptedTcpCommunicator2.init();

        encryptedTcpCommunicator1.getTcpManager().listen();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        encryptedTcpCommunicator2.getTcpManager().connect("localhost", NetworkManager.DEFAULT_PORT);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsAlive());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2IncomingHandlerIsAlive());

        encryptedTcpCommunicator1.initiateCommunication(Transformation.RSA_ECB_PKCS1Padding);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1ReceivedCommInfo2());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2ReceivedCommInfo1());
        Assert.assertEquals(1, encryptedTcpCommunicator1.getMessageQueue().size());
        Assert.assertEquals(1, encryptedTcpCommunicator2.getMessageQueue().size());
    }

    private Callable<Boolean> communicator1SessionEstablished() {
        return () -> encryptedTcpCommunicator1.sessionEstablished;
    }

    private Callable<Boolean> communicator2SessionEstablished() {
        return () -> encryptedTcpCommunicator2.sessionEstablished;
    }

    @Test
    public void Should_ProperlyGenerateAndExchangeSessionInfo_When_InitiateSessionMethodCalled()
            throws CommunicationException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
            InvalidKeyException, NoSuchPaddingException {
        encryptedTcpCommunicator1.init();
        encryptedTcpCommunicator2.init();

        encryptedTcpCommunicator1.getTcpManager().listen();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        encryptedTcpCommunicator2.getTcpManager().connect("localhost", NetworkManager.DEFAULT_PORT);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsAlive());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2IncomingHandlerIsAlive());

        encryptedTcpCommunicator1.initiateCommunication(Transformation.RSA_ECB_PKCS1Padding);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1ReceivedCommInfo2());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2ReceivedCommInfo1());

        encryptedTcpCommunicator1.initiateSession(Transformation.AES_CBC_PKCS5Padding);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1SessionEstablished());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2SessionEstablished());

        Assert.assertEquals(2, encryptedTcpCommunicator2.getMessageQueue().size());
        Assert.assertEquals(encryptedTcpCommunicator1.getSessionKey(), encryptedTcpCommunicator2.getSessionKey());
        Assert.assertArrayEquals(encryptedTcpCommunicator1.getSessionIV().getIV(),
                encryptedTcpCommunicator2.getSessionIV().getIV());
        Assert.assertEquals(encryptedTcpCommunicator1.getSymmetricTransformation(),
                encryptedTcpCommunicator2.getSymmetricTransformation());
    }

    @Test
    public void Should_ReceiveMessageWhenMessageSend_When_CommunicationAndSessionProperlyInitiated()
            throws CommunicationException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, InterruptedException {
        encryptedTcpCommunicator1.init();
        encryptedTcpCommunicator2.init();

        encryptedTcpCommunicator1.getTcpManager().listen();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        encryptedTcpCommunicator2.getTcpManager().connect("localhost", NetworkManager.DEFAULT_PORT);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1IncomingHandlerIsAlive());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2IncomingHandlerIsAlive());

        encryptedTcpCommunicator1.initiateCommunication(Transformation.RSA_ECB_PKCS1Padding);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1ReceivedCommInfo2());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2ReceivedCommInfo1());

        encryptedTcpCommunicator1.initiateSession(Transformation.AES_CBC_PKCS5Padding);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator1SessionEstablished());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(communicator2SessionEstablished());

        encryptedTcpCommunicator1.getMessageQueue().clear();
        encryptedTcpCommunicator2.getMessageQueue().clear();

        String test = "test message";
        encryptedTcpCommunicator1.sendMessage(test);

        Message message = encryptedTcpCommunicator2.getMessageQueue().poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(message);
        Assert.assertEquals(Message.Type.MESSAGE, message.messageType);
        Assert.assertEquals(test, message.data);
    }
}
