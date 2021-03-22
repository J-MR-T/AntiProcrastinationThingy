package processes;

import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class BetterTimer {
    private final Timer timer;
    private final TimerTask task;

    public BetterTimer(final Runnable runner, long period) {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(runner);
            }
        };
        timer.scheduleAtFixedRate(task, 0, period);
    }

    public void stop() {
        timer.cancel();
    }

    public void fire() {
        task.run();
    }
}
