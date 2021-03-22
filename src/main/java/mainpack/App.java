package mainpack;

import gui.GUI;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        new GUI(stage);
        stage.show();
    }
}
