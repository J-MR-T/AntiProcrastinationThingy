package mainpack;

import com.sun.javafx.application.LauncherImpl;
import processes.Process;
import processes.ProcessHandler;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Process> list = ProcessHandler.computeReducedProcessList();
        System.out.println(list.get(list.size()-1).command());
        LauncherImpl.launchApplication(App.class, args);
    }

}
