package pl.edu.pg.eti.ksr.project;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.edu.pg.eti.ksr.project.accounts.AccountManager;
import pl.edu.pg.eti.ksr.project.communication.CommunicationException;
import pl.edu.pg.eti.ksr.project.communication.EncryptedTcpCommunicator;
import pl.edu.pg.eti.ksr.project.communication.data.FileData;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.communication.data.SessionData;
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
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ResourceBundle;

public class SecondaryController implements Initializable {

    public static String FILES_PATH = "./BSK_files/file/";

    private TcpManager tcpManager;

    private EncryptionManager encryptionManager;

    @Getter
    private EncryptedTcpCommunicator communicator;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    private double progress;

    TranslateTransition arrowAnimDown;
    TranslateTransition arrowAnimUp;

    @AllArgsConstructor
    private static class TcpManagerObserver implements Observer {

        private SecondaryController controller;

        @Override
        public void update(Object o) {
            NetworkManager.Status status = (NetworkManager.Status) o;
            Platform.runLater(() -> {
                controller.changeStatusSymbol(status);
                switch (status) {
                    case CONNECTED -> {
                        controller.listenButton.setDisable(true);
                        controller.stopListenButton.setDisable(true);
                        controller.connectButton.setDisable(true);
                        controller.disconnectButton.setDisable(false);
                        controller.textChatSend.setDisable(false);
                        controller.sendingSendFile.setDisable(false);
                    }
                    case READY -> {
                        controller.listenButton.setDisable(false);
                        controller.stopListenButton.setDisable(true);
                        controller.connectButton.setDisable(false);
                        controller.disconnectButton.setDisable(true);
                        controller.textChatSend.setDisable(true);
                        controller.sendingSendFile.setDisable(true);
                    }
                    case LISTENING -> {
                        controller.listenButton.setDisable(true);
                        controller.stopListenButton.setDisable(false);
                        controller.connectButton.setDisable(false);
                        controller.disconnectButton.setDisable(true);
                        controller.textChatSend.setDisable(true);
                        controller.sendingSendFile.setDisable(true);
                    }
                }
            });
        }
    }

    @AllArgsConstructor
    private static class EncryptionManagerObserver implements Observer {

        private SecondaryController controller;

        @Override
        public void update(Object o) {
            double val = (double) o;
            if (val > controller.progress + 0.01) {
                Platform.runLater(() -> controller.updateProgress(val));
                controller.progress = val;
            }
        }
    }

    @AllArgsConstructor
    private static class CommunicatorObserver implements Observer {

        private SecondaryController controller;

        @Override
        public void update(Object o) {
            Message message = (Message) o;
            controller.communicator.getMessageQueue().poll();
            switch (message.messageType) {
                // TODO:: handle communicator messages
                case COMMUNICATION_STOP -> {
                    controller.tcpManager.disconnect();
                    controller.chatPutMessage(controller.communicator.getOtherUsername() + " exited");
                }
                case COMMUNICATION -> controller.chatPutMessage(controller.communicator.getOtherUsername() + " joined");
                case MESSAGE -> controller.chatPutMessage((String) message.data,
                        controller.communicator.getOtherUsername());
                case SESSION -> {
                    SessionData data = (SessionData) message.data;
                    Platform.runLater(() -> controller.updateSessionSettings(data.getTransformation()));
                }
                case FILE -> {
                    FileData data = (FileData) message.data;
                    controller.progress = 0;
                    Platform.runLater(() -> {
                        controller.chatPutMessage(controller.communicator.getOtherUsername() + " is sending file "
                                + data.getOriginalFileName());
                        controller.sendingAlgorithm.setDisable(true);
                        controller.sendingChoice.setDisable(true);
                        controller.sendingSendFile.setDisable(true);
                        controller.textChatSend.setDisable(true);
                        controller.stateArrow.setRotate(0.0);
                        controller.stateArrow.setVisible(true);
                        controller.stateArrow.setTranslateY(0.0);
                        controller.arrowAnimDown.play();
                    });
                }
                case FILE_READY -> {
                    FileData data = controller.communicator.getLatestFileData();
                    controller.progress = 0;
                    Platform.runLater(() -> {
                        controller.chatPutMessage("Transfer of " + data.getOriginalFileName() + " is complete");
                        controller.updateProgress(0);
                        controller.sendingAlgorithm.setDisable(false);
                        controller.sendingChoice.setDisable(false);
                        controller.sendingSendFile.setDisable(false);
                        controller.textChatSend.setDisable(false);
                        controller.arrowAnimDown.stop();
                        controller.arrowAnimUp.stop();
                        controller.stateArrow.setVisible(false);
                        controller.stateArrow.setRotate(0.0);
                    });
                }
            }
        }
    }

    @FXML
    private ProgressBar progressbarBar;

    @FXML
    private Label progressbarStatus;

    @FXML
    private ComboBox<String> sendingChoice;

    @FXML
    private ComboBox<String> sendingAlgorithm;

    @FXML
    private Button sendingFileButton;

    @FXML
    private Label sendingFileName;

    @FXML
    private Button sendingSendFile;

    @FXML
    private TextArea textChatArea;

    @FXML
    private TextField textChatMessage;

    @FXML
    private Button textChatSend;

    @FXML
    private Label connectionStatus;

    @FXML
    private Button connectButton;

    @FXML
    private TextField connectIP;

    @FXML
    private TextField connectPort;

    @FXML
    private Button disconnectButton;

    @FXML
    private Button listenButton;

    @FXML
    private TextField listenPort;

    @FXML
    private Button stopListenButton;

    @FXML
    private Label sendingDirName;

    @FXML
    private Button sendingDirectoryButton;

    @FXML
    private Button showSavedButton;

    @FXML
    private Label usernameLabel;

    @FXML
    private ImageView stateArrow;

    @FXML
    private ImageView statusImg;

    private String[] cypherModes = {"ECB", "CBC"};

    private String[] cypherAlgorithms = {"AES", "DES", "DESede"};

    private String sendFilePath = null;

    private String saveDirPath = null;

    private Image listenIcon = new Image(getClass().getResourceAsStream("ListenDefault.gif"));
    private Image readyIcon = new Image(getClass().getResourceAsStream("readyState.png"));
    private Image connectedIcon = new Image(getClass().getResourceAsStream("connectedState.png"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //Stage initializations
        stateArrow.setVisible(false);

        arrowAnimDown = new TranslateTransition();
        arrowAnimDown.setNode(stateArrow);
        arrowAnimDown.setDuration(Duration.millis(1000));
        arrowAnimDown.setCycleCount(TranslateTransition.INDEFINITE);
        arrowAnimDown.setByY(60);

        arrowAnimUp = new TranslateTransition();
        arrowAnimUp.setNode(stateArrow);
        arrowAnimUp.setDuration(Duration.millis(1000));
        arrowAnimUp.setCycleCount(TranslateTransition.INDEFINITE);
        arrowAnimUp.setByY(-60);

        sendingChoice.getItems().addAll(cypherModes);
        sendingChoice.getSelectionModel().select(0);

        sendingAlgorithm.getItems().addAll(cypherAlgorithms);
        sendingAlgorithm.getSelectionModel().select(0);

        disconnectButton.setDisable(true);
        stopListenButton.setDisable(true);
        textChatSend.setDisable(true);
        sendingSendFile.setDisable(true);

        usernameLabel.setText("\"" + AccountManager.getUsername() + "\"");

        //Communication initializations
        tcpManager = new TcpManager();
        tcpManager.attach(new TcpManagerObserver(this));
        changeStatusSymbol(tcpManager.getStatus());

        try {
            encryptionManager = new EncryptionManager(Transformation.RSA_ECB_PKCS1Padding.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        encryptionManager.attach(new EncryptionManagerObserver(this));

        publicKey = getPublicKey();
        privateKey = getPrivateKey();

        communicator = new EncryptedTcpCommunicator(FILES_PATH, AccountManager.getUsername(), publicKey, privateKey,
                Transformation.RSA_ECB_PKCS1Padding, tcpManager, encryptionManager);
        communicator.attach(new CommunicatorObserver(this));
        communicator.init();

        Path saveDir = Path.of("./BSK_files/file").toAbsolutePath();
        saveDirPath = saveDir.toString();
        sendingDirName.setText(saveDir.getFileName().toString());
        communicator.setSavedFilesPath(saveDirPath + "/");
        progressbarStatus.setText("0.0%");
        progress = 0;
    }

    @FXML
    public void updateProgress(double value) {
        BigDecimal progress = new BigDecimal(String.format("%.2f", value).replace(",", "."));
        progressbarStatus.setText((int) Math.round(progress.doubleValue() * 100) + "%");
        progressbarBar.setProgress(progress.doubleValue());
    }

    @FXML
    void connectAction(ActionEvent event) {
        if (tcpManager.connect(connectIP.getText(), Integer.parseInt(connectPort.getText()))) {
            try {
                communicator.initiateCommunication();
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void disconnectAction(ActionEvent event) {
        communicator.stopCommunication();
        tcpManager.disconnect();

        chatPutMessage("Exiting...");
    }

    @FXML
    void listenAction(ActionEvent event) {
        tcpManager.listenOn(Integer.parseInt(listenPort.getText()));
    }

    @FXML
    void stopListenAction(ActionEvent event) {
        tcpManager.stop();
    }

    @FXML
    void sendTextChatMessage(ActionEvent event) {
        if (!textChatMessage.getText().equals("") && communicator.isCommunicationEstablished()) {

            updateSession();

            chatPutMessage(textChatMessage.getText(), AccountManager.getUsername());

            try {
                communicator.send(textChatMessage.getText());
            } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
                    InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            textChatMessage.setText("");
        }
    }

    @FXML
    void showSavedAction(ActionEvent event) {
        if (saveDirPath != null) {
            try {
                Runtime.getRuntime().exec("explorer.exe " + saveDirPath);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void chooseFileToSend(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(null);

        if (f != null) {
            sendingFileName.setText(f.getName());
            sendFilePath = f.getAbsolutePath();
        }
    }

    @FXML
    void chooseDirToSave(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(null);

        if (f != null) {
            sendingDirName.setText(f.getName());
            saveDirPath = f.getAbsolutePath();
            communicator.setSavedFilesPath(saveDirPath + "/");
        }
    }

    @FXML
    void sendFile(ActionEvent event) {
        if (sendFilePath != null && communicator.isCommunicationEstablished()) {

            updateSession();

            try {
                communicator.send(Path.of(sendFilePath));
            } catch (CommunicationException | IOException | NoSuchPaddingException | NoSuchAlgorithmException |
                    BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                    InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }

            chatPutMessage("Sending file " + sendingFileName.getText() + " to " + communicator.getOtherUsername());

            sendingAlgorithm.setDisable(true);
            sendingChoice.setDisable(true);
            sendingSendFile.setDisable(true);
            textChatSend.setDisable(true);
            stateArrow.setRotate(180.0);
            stateArrow.setVisible(true);
            stateArrow.setTranslateY(60);
            arrowAnimUp.play();
        }
    }

    Transformation readSessionSettings() {
        return Transformation.fromText(sendingAlgorithm.getSelectionModel().getSelectedItem() + "/" +
                sendingChoice.getSelectionModel().getSelectedItem() + "/PKCS5Padding");
    }

    void updateSessionSettings(Transformation transformation) {
        sendingAlgorithm.setValue(transformation.getAlgorithm());
        sendingChoice.setValue(transformation.getMode());
    }

    void updateSession() {
        Transformation currentSessionSettings = readSessionSettings();

        if (!communicator.isSessionEstablished() ||
                (communicator.getSymmetricTransformation() != null &&
                        communicator.getSymmetricTransformation() != currentSessionSettings)) {
            try {
                communicator.initiateSession(currentSessionSettings);
            } catch (CommunicationException | NoSuchAlgorithmException | IllegalBlockSizeException |
                    BadPaddingException | InvalidKeyException | NoSuchPaddingException e) {
                e.printStackTrace();
            }
        }
    }

    void changeStatusSymbol(NetworkManager.Status stat) {
        switch (stat) {
            case CONNECTED -> {
                connectionStatus.setText("Connected");
                statusImg.setImage(connectedIcon);
            }
            case READY -> {
                connectionStatus.setText("Ready");
                statusImg.setImage(readyIcon);
            }
            case LISTENING -> {
                connectionStatus.setText("Listening");
                statusImg.setImage(listenIcon);
            }
        }
    }

    private void chatPutMessage(String message) {
        textChatArea.appendText("\n" + message);
    }

    private void chatPutMessage(String message, String username) {
        textChatArea.appendText(genChatMessage(message, username));
    }

    private String genChatMessage(String message, String username) {
        return "\n[" + username + "]: " + message;
    }

    private PrivateKey getPrivateKey() {
        try {
            byte[] privateKeyBytes = AccountManager.decryptFile(
                    "./BSK_files/private/" + AccountManager.getUsername() + "_encprivate.key",
                    AccountManager.getPassHash());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private PublicKey getPublicKey() {
        try {
            byte[] publicKeyBytes = AccountManager.decryptFile(
                    "./BSK_files/public/" + AccountManager.getUsername() + "_encpublic.key",
                    AccountManager.getPassHash());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
