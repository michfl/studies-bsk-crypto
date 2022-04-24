package pl.edu.pg.eti.ksr.project.network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

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
        Assert.assertNull(manager1.getServerSocket());
    }

    @Test
    public void Should_HaveStatusReady_When_StopCalledAfterListenCalled() throws InterruptedException {
        manager1.listen();
        manager1.stop();
        Thread.sleep(1000);
        Assert.assertEquals(NetworkManager.Status.READY, manager1.getStatus());
    }

    @Test
    public void Should_ReturnTrue_When_ConnectionEstablished() throws InterruptedException {
        manager1.listen();
        Thread.sleep(1000);
        boolean result = manager2.connect("localhost", NetworkManager.DEFAULT_PORT);
        Assert.assertTrue(result);
    }

    @Test
    public void Should_HaveStatusConnected_When_ConnectionEstablished() throws InterruptedException {
        manager1.listen();
        Thread.sleep(1000);
        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);
        Thread.sleep(1000);
        Assert.assertEquals(NetworkManager.Status.CONNECTED, manager1.getStatus());
        Assert.assertEquals(NetworkManager.Status.CONNECTED, manager2.getStatus());
    }

    @Test
    public void Should_HaveStatusReady_When_Disconnected1() throws InterruptedException {
        manager1.listen();
        Thread.sleep(1000);
        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);
        Thread.sleep(1000);
        manager1.disconnect();
        Assert.assertEquals(NetworkManager.Status.READY, manager1.getStatus());
    }

    @Test
    public void Should_HaveStatusReady_When_Disconnected2() throws InterruptedException {
        manager1.listen();
        Thread.sleep(1000);
        manager2.connect("localhost", NetworkManager.DEFAULT_PORT);
        Thread.sleep(1000);
        manager2.disconnect();
        Assert.assertEquals(NetworkManager.Status.READY, manager2.getStatus());
    }
}