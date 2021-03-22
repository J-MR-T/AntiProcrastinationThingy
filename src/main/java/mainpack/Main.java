package mainpack;

import com.sun.javafx.application.LauncherImpl;
import processes.ProcessHandler;

public class Main {
    //FIXME: Because the Process Objects are always recreated, starting processes again will not cause them to
    // trigger the annoying warning. Fix by making list of .command() processes which are blacklisted

    public static void main(String[] args) {
        var list = ProcessHandler.computeReducedProcessList();
        System.out.println(list.get(list.size()-1).command());
        LauncherImpl.launchApplication(App.class, args);
    }

}
