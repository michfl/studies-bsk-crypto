package pl.edu.pg.eti.ksr.project.network;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;
import pl.edu.pg.eti.ksr.project.network.data.Frame;
import pl.edu.pg.eti.ksr.project.observer.Observer;
import pl.edu.pg.eti.ksr.project.observer.Subject;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TcpManagerTest {

    private TcpManager manager1;
    private TcpManager manager2;

    @Before
    public void init() {
        manager1 = new TcpManager();
        manager2 = new TcpManager();
    }

    @After
    public void teardown() {
        manager1.stop();
        manager1.disconnect();

        manager2.stop();
        manager2.disconnect();
    }

    @Test
    public void Should_HaveStatusReady_When_Created() {
        Assert.assertEquals(NetworkManager.Status.READY, manager1.getStatus());
    }

    @Test
    public void Should_HaveStatusListening_When_ListenCalled() {
        manager1.listen();
        Assert.assertEquals(NetworkManager.Status.LISTENING, manager1.getStatus());
    }

    @Test
    public void Should_ListenOnDefaultPort_When_ListenCalled() {
        manager1.listen();
        Assert.assertEquals(NetworkManager.DEFAULT_PORT, manager1.getListeningPort());
    }

    @Test
    public void Should_ServerSocketIsNull_When_StopCalledAfterListenCalled() {
        manager1.listen();
        manager1.stop();
        Assert.assertNull(manager1.serverSocket);
    }

    private Callable<Boolean> manager1HasStatusReady() {
        return () -> manager1.getStatus() == NetworkManager.Status.READY;
    }

    @Test
    public void Should_HaveStatusReady_When_StopCalledAfterListenCalled() {
        manager1.listen();
        manager1.stop();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusReady());
    }

    private Callable<Boolean> manager1HasStatusListening() {
        return () -> manager1.getStatus() == NetworkManager.Status.LISTENING;
    }

    @Test
    public void Should_ReturnTrue_When_ConnectionEstablished() {
        manager1.listen();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        boolean result = manager2.connect("localhost", NetworkManager.DEFAULT_PORT);
        Assert.assertTrue(result);
    }

    private Callable<Boolean> manager1HasStatusConnected() {
        return () -> manager1.getStatus() == NetworkManager.Status.CONNECTED;
    }

    private Callable<Boolean> manager2HasStatusConnected() {
        return () -> manager2.getStatus() == NetworkManager.Status.CONNECTED;
    }

    @Test
    public void Should_HaveStatusConnected_When_ConnectionEstablished() {
        manager1.listen();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusConnected());
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());
    }

    @Test
    public void Should_HaveStatusReady_When_Disconnected1() {
        manager1.listen();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());

        manager1.disconnect();
        Assert.assertEquals(NetworkManager.Status.READY, manager1.getStatus());
    }

    @Test
    public void Should_HaveStatusReady_When_Disconnected2()  {
        manager1.listen();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());

        manager2.disconnect();
        Assert.assertEquals(NetworkManager.Status.READY, manager2.getStatus());
    }

    @Test
    public void Should_ReceiveData_When_DataIsSent() throws InterruptedIOException, SocketException {
        manager1.listen();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager1HasStatusListening());

        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(manager2HasStatusConnected());

        String data = "test";
        Frame receivedFrame = new Frame();
        boolean ifSent = manager1.send(new Frame(Frame.Type.TRANSFER_DATA, data));
        boolean ifReceived = manager2.receive(receivedFrame);

        Assert.assertEquals(data, receivedFrame.data);
        Assert.assertEquals(Frame.Type.TRANSFER_DATA, receivedFrame.frameType);
        Assert.assertTrue(ifSent);
        Assert.assertTrue(ifReceived);
    }

    @Test
    public void Should_NotifyObservers_When_StateChanged() {
        Observer observer = Mockito.mock(Observer.class);

        manager1.attach(observer);
        manager1.listen();
        Mockito.verify(observer)
                .update(NetworkManager.Status.LISTENING);
    }
}
