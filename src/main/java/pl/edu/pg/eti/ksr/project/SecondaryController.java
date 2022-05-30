package pl.edu.pg.eti.ksr.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import pl.edu.pg.eti.ksr.project.accounts.AccountManager;
import pl.edu.pg.eti.ksr.project.crypto.EncryptionManager;
import pl.edu.pg.eti.ksr.project.crypto.Transformation;
import pl.edu.pg.eti.ksr.project.network.NetworkManager;
import pl.edu.pg.eti.ksr.project.network.TcpManager;

import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.ResourceBundle;

public class SecondaryController implements Initializable {

    private TcpManager tcpManager;
    private EncryptionManager encryptionManager;
    private PrivateKey privateKey;
    private PublicKey publicKey;

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

        //Communication initializations
        tcpManager = new TcpManager();
        // TODO: attach observer to tcpManager
        try {
            encryptionManager = new EncryptionManager(Transformation.RSA_ECB_PKCS1Padding.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO: attach observer to encryptionManager
        PublicKey publicKey = getPublicKey();
        PrivateKey privateKey = getPrivateKey();

    }

    @FXML
    public void updateProgress(double value) {
        BigDecimal progress = new BigDecimal(String.format("%.2f", value).replace(",", "."));
        progressbarStatus.setText(progress.doubleValue() * 100 + "%");
        progressbarBar.setProgress(progress.doubleValue());
    }

    @FXML
    void connectAction(ActionEvent event) {

    }

    @FXML
    void sendTextChatMessage(ActionEvent event) {
        if (!textChatMessage.getText().equals("")) {
            String s = "" + textChatArea.getText() +
                    "\n[" + AccountManager.getUsername() + "]: " +
                    textChatMessage.getText();
            textChatArea.setText(s);
            textChatMessage.setText("");
            //Send message to the other client
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
            case CONNECTED:
                connectionSymbol.fillProperty().setValue(Color.web("0x006400"));
                connectionStatus.setText("Connected");
                break;
            case READY:
                connectionSymbol.fillProperty().setValue(Color.web("0xE6E900"));
                connectionStatus.setText("Ready");
                break;
            case LISTENING:
                connectionSymbol.fillProperty().setValue(Color.web("0xE6E900"));
                connectionStatus.setText("Listening");
                break;
        }
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
