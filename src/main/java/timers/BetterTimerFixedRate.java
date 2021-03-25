package timers;

import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class BetterTimerFixedRate extends BetterTimer {

    public BetterTimerFixedRate(final Runnable runner, long period) {
        super(new Timer(), new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(runner);
            }
        },false);
        timer.scheduleAtFixedRate(task, 0, period);
    }

}
