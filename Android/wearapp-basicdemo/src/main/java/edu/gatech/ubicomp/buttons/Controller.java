package edu.gatech.ubicomp.buttons;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jwpilly on 8/31/16.
 */
public class Controller {

    private HandlerThread controllerThread;

    public Controller(String controllerName) {
        controllerThread = new HandlerThread(controllerName);
        controllerThread.start();
    }

    public void addTimer(final Runnable task, final int milliseconds) {
        final Handler taskHandler = new Handler(controllerThread.getLooper());
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                task.run();
                taskHandler.postDelayed(this, milliseconds);
            }
        };
        taskHandler.postDelayed(taskWrapper, 0);
    }

    public void addTimerDelay(final Runnable task, final int milliseconds, final int delay) {
        final Handler taskHandler = new Handler(controllerThread.getLooper());
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                task.run();
                taskHandler.postDelayed(this, milliseconds);
            }
        };
        taskHandler.postDelayed(taskWrapper, delay);
    }
}
