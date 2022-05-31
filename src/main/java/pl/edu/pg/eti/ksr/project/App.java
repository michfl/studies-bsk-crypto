package pl.edu.pg.eti.ksr.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.edu.pg.eti.ksr.project.accounts.AccountManager;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    private static SecondaryController controller;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> closeProgram());

        AccountManager.initialize();
        stage.show();
    }

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

    private void closeProgram() {
        // TODO: sent stop communication

        controller.getCommunicator().getTcpManager().stop();
        controller.getCommunicator().getTcpManager().disconnect();
        controller.getCommunicator().close();
    }

    public static void main(String[] args) {
        launch();
    }

}