package mainpack;

import com.sun.javafx.application.LauncherImpl;
import gui.KotlinGUI;
import io.PersistenceHelper;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws IOException {
        if (Arrays.stream(args).anyMatch(str ->
                str.contains("javafx")
                        || str.contains("old")
                        || str.contains("-old"))) {
            LauncherImpl.launchApplication(App.class, args);
        } else {
            PersistenceHelper.startApp();
            new KotlinGUI().getWindow();
        }
    }

}
