package edu.gatech.glassappdisplay;

import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity {

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;

    private View mView = null;
    private SynchroView sView = null;

    private HandlerThread flashUIThread;
    private Handler flashUIHandler;

    private volatile boolean pollCommands = true;

    private Thread pollThread;

    private DatagramSocket datagramSocket = null;
    private final int PACKETSIZE = 36;
    private byte[] receiveData = new byte[PACKETSIZE];
    private DatagramPacket datagramPacket = null;

    private Runnable acceptRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), "Accept", Toast.LENGTH_LONG).show();
        }
    };

    private void runAccept() {
        runOnUiThread(acceptRunnable);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mView = buildView();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        flashUIThread = new HandlerThread("FlashUI", Process.THREAD_PRIORITY_FOREGROUND);
        flashUIThread.start();
        flashUIHandler = new Handler(flashUIThread.getLooper());

        flashUIHandler.post(new TickerRunnable(500));

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
//                return mView;
                if (mView == null) {
                    mView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_main, parent);
                    sView = (SynchroView) mView.findViewById(R.id.synchroView);
                    sView.drawRightCircle();
                }
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });
        setContentView(mCardScroller);
        try {
            datagramSocket = new DatagramSocket(5001);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        datagramPacket = new DatagramPacket(receiveData, receiveData.length);
//        runAccept();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
        pollCommands = true;
        pollThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (pollCommands) {
                    if (datagramSocket == null) {
                        pollCommands = false;
                        break;
                    }
                    try {
                        datagramSocket.receive(datagramPacket);
                        String receivedMsg = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        Log.v("receivedMsg", receivedMsg);
                        if (receivedMsg.equals("accept")) {
                            runAccept();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        pollThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCardScroller.deactivate();
        flashUIHandler.removeCallbacks(null);
        flashUIThread.quit();
        pollCommands = false;
        if (pollThread != null) {
            pollThread.interrupt();
            try {
                pollThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Runnable drawLeftCircle = new Runnable() {
        @Override
        public void run() {
            if (sView == null) {
                return;
            }
            sView.drawLeftCircle();
        }
    };

    public Runnable drawRightCircle = new Runnable() {
        @Override
        public void run() {
            if (sView == null) {
                return;
            }
            sView.drawRightCircle();
        }
    };

    private long lastStartTime = 0;
    private boolean isLeft = false;

    private class TickerRunnable implements Runnable {
        private int periodTime;
        private long startTime;
        private long waitTime;
        private long postDrawTime;


        public TickerRunnable(int periodTime) {
            this.periodTime = periodTime;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            long lastTime = startTime;
            long currentTime = 0;
//            for (int i = 0; i < Config.NUM_CYCLES * 2; i++) {
            int i = 0;
            while (true) {
                long nextStartTime = startTime + (periodTime * i);

                currentTime = System.currentTimeMillis();
                long actualTime = currentTime - lastTime;
                lastTime = currentTime;
                if (isLeft) {
                    runOnUiThread(drawLeftCircle);
                    postDrawTime = System.currentTimeMillis();
                } else {
                    runOnUiThread(drawRightCircle);
                    postDrawTime = System.currentTimeMillis();
                }
                isLeft = !isLeft;

                waitTime = periodTime - (postDrawTime - nextStartTime);
                long cycleTime = periodTime + waitTime;
                if (Math.abs(cycleTime) > periodTime) cycleTime = periodTime;
                lastStartTime = startTime;
                i++;
                try {
                    Thread.sleep(cycleTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
