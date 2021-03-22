package timers;

import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class BetterTimerExecuteOnce {
    private final Timer timer;
    private final TimerTask task;

    public BetterTimerExecuteOnce(Runnable runTask, long delay) {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(runTask);
                stop();
            }
        };
        timer.schedule(task,delay);
    }

    public void stop() {
        timer.cancel();
    }

    public void fire() {
        task.run();
    }
}
