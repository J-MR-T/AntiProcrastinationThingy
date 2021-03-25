package timers;

import java.util.Timer;
import java.util.TimerTask;

public abstract class BetterTimer {
    protected final Timer timer;
    protected final TimerTask task;

    protected BetterTimer(Timer timer, TimerTask task,final boolean stopAtEnd) {
        this.timer = timer;
        this.task = new TimerTask() {
            @Override
            public void run() {
                task.run();
                if(stopAtEnd){
                    stop();
                }
            }
        };
    }

    public void stop() {
        timer.cancel();
    }

    public void fire() {
        task.run();
    }
}
