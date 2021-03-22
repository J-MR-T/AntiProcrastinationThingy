package timers;

import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class BetterTimerExecuteOnce {
    private final Timer timer;
    private final TimerTask task;

    public BetterTimerExecuteOnce(final Runnable runner, long delay) {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(runner);
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
