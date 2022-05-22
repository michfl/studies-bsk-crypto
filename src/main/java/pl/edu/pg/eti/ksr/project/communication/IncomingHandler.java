package pl.edu.pg.eti.ksr.project.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.communication.data.CommunicationData;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.network.data.CommunicationInfo;
import pl.edu.pg.eti.ksr.project.network.data.FileInfo;
import pl.edu.pg.eti.ksr.project.network.data.Frame;
import pl.edu.pg.eti.ksr.project.network.data.SessionInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receives and handles incoming messages depending on frame type.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IncomingHandler implements Runnable {

    /**
     * Reference to communicator object.
     */
    private EncryptedTcpCommunicator communicator;

    /**
     * Running flag used for stopping the thread.
     */
    private AtomicBoolean running;

    @Override
    public void run() {
        Frame frame;

        int commInitState = 1;
        String otherUsername = "";
        PublicKey otherPublicKey = null;
        String challenge = "";

        while (running.get()) {
            frame = new Frame();

            try {
                if (!communicator.tcpManager.receive(frame)) continue;

                switch (frame.frameType) {

                    case COMMUNICATION_INIT -> {
                        CommunicationInfo info = (CommunicationInfo) frame.data;

                        if (info.getNum() == 0) {

                            otherUsername = info.getUsername();
                            otherPublicKey = info.getUserPublicKey();
                            communicator.encryptionManager.setTransformation(info.getAsymmetricAlgorithm());
                            challenge = EncryptedTcpCommunicator.generateChallenge();

                            info.setUsername(communicator.username);
                            info.setUserPublicKey(communicator.userPublicKey);
                            info.setChallenge(challenge);

                            info.setNum(commInitState);
                            commInitState += 1;

                            frame.data = info;
                            communicator.tcpManager.send(frame);

                        } else if (info.getNum() == commInitState && commInitState == 1) {

                            otherUsername = info.getUsername();
                            otherPublicKey = info.getUserPublicKey();
                            communicator.encryptionManager.setTransformation(info.getAsymmetricAlgorithm());
                            challenge = EncryptedTcpCommunicator.generateChallenge();

                            info.setUsername(communicator.username);
                            info.setUserPublicKey(communicator.userPublicKey);
                            info.setChallengeResponse(communicator.encryptionManager
                                    .encrypt(info.getChallenge(), communicator.userPrivateKey));
                            info.setChallenge(challenge);

                            info.setNum(commInitState + 1);
                            commInitState += 2;

                            frame.data = info;
                            communicator.tcpManager.send(frame);

                        } else if (info.getNum() == commInitState && commInitState == 2) {

                            if (!Objects.equals(info.getUsername(), otherUsername) ||
                                    info.getUserPublicKey() != otherPublicKey ||
                                    !Objects.equals(communicator.encryptionManager
                                            .decrypt(info.getChallengeResponse(), otherPublicKey), challenge)) {

                                commInitState = 1;
                                otherUsername = "";
                                otherPublicKey = null;
                                challenge = "";
                                continue;
                            }

                            info.setUsername(communicator.username);
                            info.setUserPublicKey(communicator.userPublicKey);
                            info.setChallengeResponse(communicator.encryptionManager
                                    .encrypt(info.getChallenge(), communicator.userPrivateKey));

                            info.setNum(commInitState + 1);
                            commInitState += 2;

                            frame.data = info;
                            communicator.tcpManager.send(frame);

                        } else if (info.getNum() == commInitState && commInitState == 3) {

                            if (Objects.equals(info.getUsername(), otherUsername) &&
                                    info.getUserPublicKey() == otherPublicKey &&
                                    Objects.equals(communicator.encryptionManager
                                            .decrypt(info.getChallengeResponse(), otherPublicKey), challenge)) {

                                info.setUsername(communicator.username);
                                info.setUserPublicKey(communicator.userPublicKey);
                                info.setNum(commInitState + 1);

                                frame.data = info;
                                communicator.tcpManager.send(frame);
                            }

                            // the starting side of the handshake is done and confirmed
                            communicator.otherUsername = otherUsername;
                            communicator.otherUserPublicKey = otherPublicKey;
                            communicator.newMessage(Message.Type.COMMUNICATION,
                                    new CommunicationData(otherUsername, otherPublicKey));


                            commInitState = 1;
                            otherUsername = "";
                            otherPublicKey = null;
                            challenge = "";

                        } else if (info.getNum() == commInitState && commInitState == 4) {

                            if (Objects.equals(info.getUsername(), otherUsername) &&
                                    info.getUserPublicKey() == otherPublicKey) {

                                // the receiving side of the handshake is done and confirmed
                                communicator.otherUsername = otherUsername;
                                communicator.otherUserPublicKey = otherPublicKey;
                                communicator.newMessage(Message.Type.COMMUNICATION,
                                        new CommunicationData(otherUsername, otherPublicKey));
                            }

                            commInitState = 1;
                            otherUsername = "";
                            otherPublicKey = null;
                            challenge = "";

                        }
                    }

                    case COMMUNICATION_STOP -> {

                    }

                    case SESSION_INIT -> {
                        SessionInfo info = (SessionInfo) frame.data;
                    }

                    case MESSAGE -> {
                        String message = (String) frame.data;
                    }

                    case TRANSFER_INIT -> {
                        FileInfo info = (FileInfo) frame.data;
                    }

                    case TRANSFER_DATA -> {
                        byte[] data = (byte[]) frame.data;
                    }

                }
            } catch (InterruptedIOException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException |
                    BadPaddingException | InvalidKeyException e) {

                e.printStackTrace();
                commInitState = 1;
                otherUsername = "";
                otherPublicKey = null;
                challenge = "";
            } catch (SocketException | InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
