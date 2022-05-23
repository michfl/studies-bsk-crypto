package pl.edu.pg.eti.ksr.project.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.pg.eti.ksr.project.communication.data.CommunicationData;
import pl.edu.pg.eti.ksr.project.communication.data.FileData;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.communication.data.SessionData;
import pl.edu.pg.eti.ksr.project.network.data.CommunicationInfo;
import pl.edu.pg.eti.ksr.project.network.data.FileInfo;
import pl.edu.pg.eti.ksr.project.network.data.Frame;
import pl.edu.pg.eti.ksr.project.network.data.SessionInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
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
                            communicator.encryptionManager.setTransformation(
                                    communicator.getAsymmetricTransformation().getText());
                            challenge = EncryptedTcpCommunicator.generateChallenge();

                            info.setUsername(communicator.username);
                            info.setUserPublicKey(communicator.userPublicKey);
                            info.setChallenge(challenge);

                            info.setNum(info.getNum() + 1);

                            frame.data = info;
                            communicator.tcpManager.send(frame);

                        } else if (info.getNum() == 1) {

                            otherUsername = info.getUsername();
                            otherPublicKey = info.getUserPublicKey();
                            communicator.encryptionManager.setTransformation(
                                    communicator.getAsymmetricTransformation().getText());
                            challenge = EncryptedTcpCommunicator.generateChallenge();

                            info.setUsername(communicator.username);
                            info.setUserPublicKey(communicator.userPublicKey);
                            info.setChallengeResponse(communicator.encryptionManager
                                    .encrypt(info.getChallenge(), communicator.userPrivateKey));
                            info.setChallenge(challenge);

                            info.setNum(info.getNum() + 1);

                            frame.data = info;
                            communicator.tcpManager.send(frame);

                        } else if (info.getNum() == 2) {

                            if (!Objects.equals(info.getUsername(), otherUsername) ||
                                    info.getUserPublicKey() != otherPublicKey ||
                                    !Objects.equals(communicator.encryptionManager
                                            .decrypt(info.getChallengeResponse(), otherPublicKey), challenge)) {

                                otherUsername = "";
                                otherPublicKey = null;
                                challenge = "";
                                continue;
                            }

                            info.setUsername(communicator.username);
                            info.setUserPublicKey(communicator.userPublicKey);
                            info.setChallengeResponse(communicator.encryptionManager
                                    .encrypt(info.getChallenge(), communicator.userPrivateKey));

                            info.setNum(info.getNum() + 1);

                            frame.data = info;
                            communicator.tcpManager.send(frame);

                        } else if (info.getNum() == 3) {

                            if (Objects.equals(info.getUsername(), otherUsername) &&
                                    info.getUserPublicKey() == otherPublicKey &&
                                    Objects.equals(communicator.encryptionManager
                                            .decrypt(info.getChallengeResponse(), otherPublicKey), challenge)) {

                                info.setUsername(communicator.username);
                                info.setUserPublicKey(communicator.userPublicKey);
                                info.setNum(info.getNum() + 1);

                                frame.data = info;
                                communicator.tcpManager.send(frame);
                            }

                            // the starting side of the handshake is done and confirmed
                            communicator.otherUsername = otherUsername;
                            communicator.otherUserPublicKey = otherPublicKey;
                            communicator.communicationEstablished = true;
                            communicator.newMessage(Message.Type.COMMUNICATION,
                                    new CommunicationData(otherUsername));

                            otherUsername = "";
                            otherPublicKey = null;
                            challenge = "";

                        } else if (info.getNum() == 4) {

                            if (Objects.equals(info.getUsername(), otherUsername) &&
                                    info.getUserPublicKey() == otherPublicKey) {

                                // the receiving side of the handshake is done and confirmed
                                communicator.otherUsername = otherUsername;
                                communicator.otherUserPublicKey = otherPublicKey;
                                communicator.communicationEstablished = true;
                                communicator.newMessage(Message.Type.COMMUNICATION,
                                        new CommunicationData(otherUsername));
                            }

                            otherUsername = "";
                            otherPublicKey = null;
                            challenge = "";
                        }
                    }

                    case COMMUNICATION_STOP -> {
                        communicator.newMessage(Message.Type.COMMUNICATION_STOP, null);
                        communicator.communicationEstablished = false;
                        communicator.sessionEstablished = false;
                        if (communicator.cyphering) communicator.stopCyphering();
                    }

                    case SESSION_INIT -> {
                        if (!communicator.communicationEstablished) continue;
                        if (communicator.cyphering) continue;

                        SessionInfo info = (SessionInfo) frame.data;

                        communicator.encryptionManager.setTransformation(
                                communicator.asymmetricTransformation.getText());

                        communicator.sessionKey = communicator.encryptionManager.decrypt(info.getEncryptedSessionKey(),
                                communicator.userPrivateKey, info.getTransformation().getAlgorithm());
                        communicator.symmetricTransformation = info.getTransformation();
                        communicator.sessionIV = new IvParameterSpec(info.getIv());
                        communicator.sessionEstablished = true;

                        communicator.newMessage(Message.Type.SESSION, new SessionData(info.getTransformation()));
                    }

                    case MESSAGE -> {
                        if (!communicator.sessionEstablished) continue;

                        if (!Objects.equals(communicator.encryptionManager.getTransformation(),
                                communicator.symmetricTransformation.getText())) {

                            communicator.encryptionManager.setTransformation(
                                    communicator.symmetricTransformation.getText());
                        }

                        byte[] encMessage = (byte[]) frame.data;
                        String message;

                        if (Objects.equals(communicator.symmetricTransformation.getMode(), "CBC")) {
                            message = communicator.encryptionManager.decrypt(
                                    encMessage, communicator.sessionKey, communicator.sessionIV);
                        } else {
                            message = communicator.encryptionManager.decrypt(encMessage, communicator.sessionKey);
                        }

                        communicator.newMessage(Message.Type.MESSAGE, message);
                    }

                    case TRANSFER_INIT -> {
                        if (communicator.cyphering) continue;

                        FileInfo info = (FileInfo) frame.data;

                        if (!Objects.equals(communicator.encryptionManager.getTransformation(),
                                communicator.symmetricTransformation.getText())) {

                            communicator.encryptionManager.setTransformation(
                                    communicator.symmetricTransformation.getText());
                        }

                        String originalFileName;
                        long originalFileSize;

                        if (Objects.equals(communicator.symmetricTransformation.getMode(), "CBC")) {
                            originalFileName = communicator.encryptionManager.decrypt(info.getFileName(),
                                    communicator.sessionKey, communicator.sessionIV);
                            originalFileSize = Long.parseLong(communicator.encryptionManager.decrypt(info.getFileSize(),
                                    communicator.sessionKey, communicator.sessionIV));
                        } else {
                            originalFileName = communicator.encryptionManager.decrypt(info.getFileName(),
                                    communicator.sessionKey);
                            originalFileSize = Long.parseLong(communicator.encryptionManager.decrypt(info.getFileSize(),
                                    communicator.sessionKey));
                        }

                        String filePath;
                        String newFileName;

                        if (new File(communicator.savedFilesPath + originalFileName).exists()) {
                            int i = 0;
                            String[] fileNameParts = originalFileName.split("\\.");
                            String name = fileNameParts[0];

                            do {
                                i++;
                                fileNameParts[0] = name + "_" + i;
                            } while (new File(communicator.savedFilesPath + String.join(".",
                                    fileNameParts)).exists());

                            filePath = communicator.savedFilesPath + String.join(".", fileNameParts);
                            newFileName = String.join(".", fileNameParts);
                        } else {
                            filePath = communicator.savedFilesPath + originalFileName;
                            newFileName = originalFileName;
                        }

                        FileData fileData = new FileData(originalFileName, newFileName, filePath);
                        communicator.latestFileData = fileData;
                        communicator.cyphering = true;
                        communicator.filePartQueue.clear();

                        if (Objects.equals(communicator.symmetricTransformation.getMode(), "CBC")) {
                            communicator.encryptionManager.decrypt(communicator.filePartQueue, Path.of(filePath),
                                    communicator.sessionKey, communicator.sessionIV, originalFileSize);
                        } else {
                            communicator.encryptionManager.decrypt(communicator.filePartQueue, Path.of(filePath),
                                    communicator.sessionKey, originalFileSize);
                        }

                        communicator.newMessage(Message.Type.FILE, fileData);
                    }

                    case TRANSFER_DATA -> {
                        if (!communicator.cyphering) continue;

                        byte[] data = (byte[]) frame.data;
                        communicator.filePartQueue.put(data);

                        if (data.length == 0) {
                            communicator.encryptionManager.getEncryptorThread().join();
                            communicator.newMessage(Message.Type.FILE_READY, null);
                            communicator.cyphering = false;
                        }
                    }

                }
            } catch (InterruptedIOException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException |
                    BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {

                e.printStackTrace();
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
