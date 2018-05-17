package edu.gatech.glassappdisplay;

import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.widget.AdapterView;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

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

    private int syncNumber = 0;

    private AudioManager mAudioManager = null;

    private long startTime = 0;

    private String baseDir = null;
    private File saveDir = null;

    private ArrayList<String> writeData = new ArrayList<String>();

    private class HideRunnable implements Runnable {

        private int sNumber;

        public HideRunnable(int sNumber) {
            this.sNumber = sNumber;
        }

        @Override
        public void run() {
            if (!pollCommands) {
                return;
            }
            if (syncNumber > sNumber) {
                return;
            }
            if (!sView.getShowNotif()) {
                return;
            }
            long elapsedTime = System.currentTimeMillis() - startTime;
            Toast.makeText(getApplicationContext(), "Missed " + syncNumber + " : " + elapsedTime, Toast.LENGTH_SHORT).show();
            writeData.add("" + System.currentTimeMillis() + ",missed," + syncNumber + "," + elapsedTime);
            syncNumber++;
            sView.setShowNotif(false);
            notificationHandler.postDelayed(notificationRunnable, getPoissonRandom((Config.NOTIF_DELAY / 1000)));
        }
    }

    private Runnable acceptRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pollCommands) {
                return;
            }
            if (!sView.getShowNotif()) {
                return;
            }
            long elapsedTime = System.currentTimeMillis() - startTime;
            Toast.makeText(getApplicationContext(), "Dismissed " + syncNumber + " : " + elapsedTime, Toast.LENGTH_SHORT).show();
            writeData.add("" + System.currentTimeMillis() + ",dismissed," + syncNumber + "," + elapsedTime);
            syncNumber++;
            sView.setShowNotif(false);
            notificationHandler.postDelayed(notificationRunnable, getPoissonRandom((Config.NOTIF_DELAY / 1000)));
        }
    };

    private void runAccept() {
        runOnUiThread(acceptRunnable);
    }

    private class SaveOutputTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {
            if (writeData.size() == 0) {
                return true;
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            Date today = Calendar.getInstance().getTime();
            String reportDate = df.format(today);
            File outputFile = new File(baseDir, reportDate + ".csv");
            ArrayList<String> writeDataCopy = new ArrayList<String>(writeData);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outputFile);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                for (String line : writeDataCopy) {
                    bw.write(line);
                    bw.newLine();
                }
                bw.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            writeData.clear();
            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.d("SaveOutputTask", "completed save output data " + result);
        }
    }

    private Handler notificationHandler = new Handler();

    private Runnable notificationRunnable = new Runnable() {
        @Override
        public void run() {
            if (syncNumber >= Config.SYNC_NUM) {
                writeData.add("" + System.currentTimeMillis() + ",finish");
            }
            new SaveOutputTask().execute();
            if (syncNumber >= Config.SYNC_NUM) {
                Toast.makeText(getApplicationContext(), "Finished!", Toast.LENGTH_LONG).show();
                pollCommands = false;
                pollThread.interrupt();
                finish();
                return;
            }
            Log.v("nr", "nr");
            if (!pollCommands) {
                return;
            }
            if (mAudioManager != null) {
                mAudioManager.playSoundEffect(Sounds.SUCCESS);
            }

            startTime = System.currentTimeMillis();
            writeData.add("" + startTime + ",show");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sView.setShowNotif(true);
                }
            });
            Log.v("nhPost", "syncNumber " + syncNumber);
            notificationHandler.postDelayed(new HideRunnable(syncNumber), Config.NOTIF_TIME);
        }
    };


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date today = Calendar.getInstance().getTime();
        String reportDate = df.format(today);
        baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
                + "/Synchro/" + reportDate + "/";
        saveDir = new File(baseDir);
        if (!saveDir.exists()) {
            Log.v("onCreate", "creating saveDir");
            saveDir.mkdirs();
        }

        flashUIThread = new HandlerThread("FlashUI", Process.THREAD_PRIORITY_FOREGROUND);
        flashUIThread.start();
        flashUIHandler = new Handler(flashUIThread.getLooper());

        flashUIHandler.post(new TickerRunnable(500));

        writeData.add("" + System.currentTimeMillis() + ",start");

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

        notificationHandler.postDelayed(notificationRunnable, Config.NOTIF_TIME);
//        runAccept();

        initSocket();
        mCardScroller.activate();
        pollCommands = true;
        pollThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (pollCommands) {
                    if (datagramSocket == null) {
                        initSocket();
                    }
                    try {
                        datagramSocket.receive(datagramPacket);
                        String receivedMsg = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        Log.v("receivedMsg", receivedMsg);
                        if (receivedMsg.equals("accept")) {
                            runAccept();
                        }
                    } catch (IOException e) {
                        initSocket();
                        e.printStackTrace();
                    }
                }
            }
        });
        pollThread.start();
    }

    private void closeSocket() {
        if (datagramSocket != null) {
            if (datagramSocket.isConnected()) {
                datagramSocket.disconnect();
            }
            if (!datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    public static int getPoissonRandom(double seconds) {
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

    private void initSocket() {
        closeSocket();
        try {
            datagramSocket = new DatagramSocket(5001);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        datagramPacket = new DatagramPacket(receiveData, receiveData.length);
        Toast.makeText(getApplicationContext(), "IP: " + Utils.getIPAddress(true), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSocket();
        pollCommands = false;
        mCardScroller.deactivate();
        flashUIHandler.removeCallbacks(null);
        flashUIThread.quit();
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
            if (!pollCommands) {
                return;
            }
            long lastTime = startTime;
            long currentTime = 0;
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
