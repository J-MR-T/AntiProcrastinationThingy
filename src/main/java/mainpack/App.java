package mainpack;

import gui.GUI;
import io.GsonHelper;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        try {
            GsonHelper.startApp();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new GUI(stage);
        stage.show();
    }
}
