package pl.edu.pg.eti.ksr.project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.edu.pg.eti.ksr.project.accounts.AccountManager;
import pl.edu.pg.eti.ksr.project.communication.EncryptedTcpCommunicator;
import pl.edu.pg.eti.ksr.project.communication.data.Message;
import pl.edu.pg.eti.ksr.project.crypto.EncryptionManager;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;
import pl.edu.pg.eti.ksr.project.observer.Observer;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
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
                        controller.connectButton.setDisable(true);
                        controller.disconnectButton.setDisable(false);
                    }
                    case READY, LISTENING -> {
                        controller.listenButton.setDisable(false);
                        controller.connectButton.setDisable(false);
                        controller.disconnectButton.setDisable(true);
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
            controller.updateProgress((double) o);
        }
    }

    @AllArgsConstructor
    private static class CommunicatorObserver implements Observer {

        private SecondaryController controller;

        @Override
        public void update(Object o) {
            Message message = (Message) o;
            switch (message.messageType) {
                // TODO:: handle communicator messages
                case COMMUNICATION_STOP -> controller.tcpManager.disconnect();
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
    private Circle connectionSymbol;

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

    private String[] cypherModes = {"ECB", "CBC"};
    private String[] cypherAlgorithms = {"AES", "DES", "3DES"};
    private String sendFilePath = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //Stage initializations
        sendingChoice.getItems().addAll(cypherModes);
        sendingChoice.getSelectionModel().select(0);

        sendingAlgorithm.getItems().addAll(cypherAlgorithms);
        sendingAlgorithm.getSelectionModel().select(0);

        disconnectButton.setDisable(true);

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
    }

    @FXML
    public void updateProgress(double value) {
        BigDecimal progress = new BigDecimal(String.format("%.2f", value).replace(",", "."));
        progressbarStatus.setText(progress.doubleValue() * 100 + "%");
        progressbarBar.setProgress(progress.doubleValue());
    }

    @FXML
    void connectAction(ActionEvent event) {
        tcpManager.connect(connectIP.getText(), Integer.parseInt(connectPort.getText()));
    }

    @FXML
    void disconnectAction(ActionEvent event) {
        communicator.stopCommunication();
        tcpManager.disconnect();
    }

    @FXML
    void listenAction(ActionEvent event) {
        tcpManager.listenOn(Integer.parseInt(listenPort.getText()));
    }

    @FXML
    void sendTextChatMessage(ActionEvent event) {
        if (!textChatMessage.getText().equals("")) {
            String mess = genChatMessage(textChatMessage.getText(), AccountManager.getUsername());
            textChatArea.setText(mess);
            textChatMessage.setText("");
            //Send mess to the other client
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
    void sendFile(ActionEvent event) {
        if (sendFilePath != null && sendingChoice.getValue() != null) {
            //Send file to the other client
        }
    }

    void changeStatusSymbol(NetworkManager.Status stat) {
        if (!connectionSymbol.isVisible())
            connectionSymbol.setVisible(true);
        switch (stat) {
            case CONNECTED -> {
                connectionSymbol.fillProperty().setValue(Color.web("0x006400"));
                connectionStatus.setText("Connected");
            }
            case READY -> {
                connectionSymbol.fillProperty().setValue(Color.web("0xE6E900"));
                connectionStatus.setText("Ready");
            }
            case LISTENING -> {
                connectionSymbol.fillProperty().setValue(Color.web("0xE6E900"));
                connectionStatus.setText("Listening");
            }
        }
    }

    private String genChatMessage(String message, String username) {
        return "" + textChatArea.getText() +
                "\n[" + username + "]: " +
                message;
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
        File publicKeyFile = new File("./BSK_files/public/" + AccountManager.getUsername() + "_encpublic.key");
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
