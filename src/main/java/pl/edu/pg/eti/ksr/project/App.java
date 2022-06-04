package pl.edu.pg.eti.ksr.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import pl.edu.pg.eti.ksr.project.accounts.AccountManager;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {

    /**
     * Current scene.
     */
    private static Scene scene;

    /**
     * Reference to the secondary controller.
     * Used for gracefully stopping the application.
     */
    private static SecondaryController controller;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 600, 420);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("CryptoSender");
        Image icon = new Image(getClass().getResourceAsStream("stageIcon2.png"));
        stage.getIcons().add(icon);

        stage.setOnCloseRequest(e -> closeProgram());

        AccountManager.initialize();
        stage.show();
    }

    /**
     * Used for changing current scene root.
     * @param fxml fxml string
     */
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent parent = fxmlLoader.load();
        if  (Objects.equals(fxml, "secondary")) {
            controller = fxmlLoader.getController();
        }
        return parent;
    }

    /**
     * Gracefully stops application.
     */
    private void closeProgram() {
        if (controller != null) {
            if (controller.getCommunicator().isCommunicationEstablished())
                controller.getCommunicator().stopCommunication();
            controller.getCommunicator().getTcpManager().stop();
            controller.getCommunicator().getTcpManager().disconnect();
            controller.getCommunicator().close();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}