package edu.gatech.ubicomp.synchro.livedatacollect;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Random;

/**
 * Created by jwpilly on 8/31/16.
 */
public class Controller {

    private HandlerThread controllerThread;

    public Controller(String controllerName) {
        controllerThread = new HandlerThread(controllerName);
        controllerThread.start();
    }

    public void killAll() {
        try {
            controllerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    public void addTimerEnd(final Runnable task, final int milliseconds, final long length, final Runnable endTask) {
		Log.v("debug", "addTimerEnd");
        final Handler taskHandler = new Handler(controllerThread.getLooper());
        final long startTime = System.currentTimeMillis();
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
				long timeDelta = System.currentTimeMillis() - startTime;
				Log.v("timedelta", ""+timeDelta);
                if (timeDelta < length) {
					Log.v("debug", "inside timer");
                    task.run();
                    taskHandler.postDelayed(this, milliseconds);
                } else {
                    endTask.run();
                }
            }
        };
        taskHandler.postDelayed(taskWrapper, 0);
    }

    public int getPoissonRandom(double seconds) {
        Random r = new Random();
        double L = Math.exp(-seconds);
        int k = 0;
        double p = 1.0;
        do {
            p = p * r.nextDouble();
            k++;
        } while (p > L);
        return (k - 1) * 1000;
    }

    public void addTimerDelayPoisson(final Runnable task, final int secondsTimer, final int secondsDelay) {
        final Handler taskHandler = new Handler(controllerThread.getLooper());
        Runnable taskWrapper = new Runnable() {
            @Override
            public void run() {
                task.run();
                int delayTime = getPoissonRandom(secondsTimer)*1000;
                Log.v("delay", "" + delayTime);
                taskHandler.postDelayed(this, delayTime);
            }
        };
        int delayTime = getPoissonRandom(secondsDelay)*1000;
        Log.v("initial delay", "" + delayTime);
        taskHandler.postDelayed(taskWrapper, delayTime);
    }
}
