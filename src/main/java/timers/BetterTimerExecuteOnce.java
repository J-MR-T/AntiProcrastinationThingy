package timers;

import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class BetterTimerExecuteOnce extends BetterTimer {

    public BetterTimerExecuteOnce(Runnable runTask, long delay) {
        super(new Timer(), new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(runTask);
            }
        },true);
        timer.schedule(task, delay);
    }

    public BetterTimerExecuteOnce(Runnable runTask) {
        this(runTask, 0L);
    }
}
