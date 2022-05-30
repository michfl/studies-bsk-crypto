package pl.edu.pg.eti.ksr.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import pl.edu.pg.eti.ksr.project.accounts.AccountManager;

import java.io.IOException;

public class PrimaryController {

    @FXML
    private Label communicate;

    @FXML
    private Button logIn;

    @FXML
    private TextField passwordInput;

    @FXML
    private Button signIn;

    @FXML
    private TextField usernameInput;

    @FXML
    void logInClicked(ActionEvent event) {
        String username = usernameInput.getText();
        if (AccountManager.getUsers().containsKey(username)) {
            Integer passHash = passwordInput.getText().hashCode();
            if (AccountManager.getUsers().get(username).equals(passHash)) {
                communicate.setText("Logged as: " + username + "!");
                communicate.setTextFill(Color.web("0x006400"));
                AccountManager.setUsername(username);
                AccountManager.setPassHash(passHash);
                try {
                    this.switchToSecondary();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                communicate.setText("Password is wrong!");
                communicate.setTextFill(Color.color(1, 0, 0));
            }
        } else {
            communicate.setText("There is no such user!");
            communicate.setTextFill(Color.color(1, 0, 0));
        }
    }

    @FXML
    void signInClicked(ActionEvent event) {
        if (AccountManager.getUsers().containsKey(usernameInput.getText())) {
            communicate.setText("Account with that username exists");
            communicate.setTextFill(Color.color(1, 0, 0));
        } else {
            AccountManager.addAccount(usernameInput.getText(), passwordInput.getText().hashCode());
            communicate.setText("Added account");
            communicate.setTextFill(Color.web("0x006400"));
        }
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

}
