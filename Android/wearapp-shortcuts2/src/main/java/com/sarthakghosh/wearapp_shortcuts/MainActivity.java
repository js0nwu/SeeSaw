package com.sarthakghosh.wearapp_shortcuts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.fft.FFT;
import edu.gatech.ubicomp.whoosh.recognizer.AudioService;
import edu.gatech.ubicomp.whoosh.recognizer.NVector;
import edu.gatech.ubicomp.whoosh.recognizer.SimpleRecognizer;

public class MainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private TextView mTextView;
    private ImageView mkeep;
    private ImageView accuweather;
    private ImageView mWater;
    private ImageView mFit;
    private ImageView mGoogle;
    private ImageView mHangouts;
    private ImageView mFindPhone;
    private ImageView mMaps;
    private GoogleApiClient mApiClient;
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String FIT_PACKAGE = "com.google.android.apps.fitness";
    private static final String HANGOUTS_PACKAGE = "com.google.android.talk";
    private static final String GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String KEEP_PACKAGE = "com.google.android.keep";
    private static final String WATER_PACKAGE = "com.northpark.drinkwater";
    private static final String FIND_PHONE_PACKAGE = "com.google.android.apps.adm";
    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";
    private static final String WEATHER_PACKAGE = "com.accuweather.android";
    String s = "";


    private SimpleRecognizer simpleRecognizer;
    private AudioProcessor mAudioProcessor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        initGoogleApiClient();
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mkeep = (ImageView) findViewById(R.id.alarm);
                accuweather = (ImageView) findViewById(R.id.calendar);
                mWater = (ImageView) findViewById(R.id.compass);
                mFit = (ImageView) findViewById(R.id.fit);
                mGoogle = (ImageView) findViewById(R.id.google);
                mHangouts = (ImageView) findViewById(R.id.hangouts);
                mFindPhone = (ImageView) findViewById(R.id.heartrate);
                mMaps = (ImageView) findViewById(R.id.maps);
            }
        });

        simpleRecognizer = new SimpleRecognizer();
        simpleRecognizer.setEventRecognitionListener(new SimpleRecognizer.EventRecognitionListener() {
            @Override
            public void onEventRecognized(final String result) {
                Log.d("output", result);
                if (result.equals("short blow")) {

                } else if (result.equals("long blow")) {

                } else if (result.equals("double blow")) {

                }
            }

        });

    }


    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting()))
            mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        s = new String(messageEvent.getData());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {

                    Log.d(MainActivity.class.getSimpleName(), "message received :" + s);
                    switch (s) {
                        case "1":
                            getAnimationForImageView(mkeep).start();
                            break;
                        case "2":
                            getAnimationForImageView(accuweather).start();
                            break;
                        case "3":
                            getAnimationForImageView(mWater).start();
                            break;
                        case "4":
                            getAnimationForImageView(mFit).start();
                            break;
                        case "5":
                            getAnimationForImageView(mGoogle).start();
                            break;
                        case "6":
                            getAnimationForImageView(mHangouts).start();
                            break;
                        case "7":
                            getAnimationForImageView(mFindPhone).start();
                            break;
                        case "8":
                            getAnimationForImageView(mMaps).start();
                            break;
                        default:
                            break;
                    }

                }
            }
        });

    }

    private ValueAnimator getAnimationForImageView(final ImageView imageView) {
        final ValueAnimator scaleAnimator, scaleUp;
        scaleAnimator = ValueAnimator.ofFloat(1f, 0.5f);
        scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimator.setDuration(200);
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                imageView.setScaleX(animatedValue);
                imageView.setScaleY(animatedValue);
            }
        });

        scaleUp = ValueAnimator.ofFloat(0.5f, 1.0f);
        scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleUp.setDuration(200);
        scaleUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                imageView.setScaleX(animatedValue);
                imageView.setScaleY(animatedValue);

            }
        });
        scaleAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                scaleUp.start();


            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        scaleUp.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                //launch the app from here.


                if (s.equals("1")) {
                    //google keep
                    openApp(MainActivity.this, KEEP_PACKAGE);
                } else if (s.equals("2")) {
                    //google kweather
                    Log.d("wear main activity", " Weather package launched");
                    openApp(MainActivity.this, WEATHER_PACKAGE);
                } else if (s.equals("3")) {
                    //google water
                    openApp(MainActivity.this, WATER_PACKAGE);
                } else if (s.equals("4")) {
                    //google fit
                    openApp(MainActivity.this, FIT_PACKAGE);
                } else if (s.equals("5")) {
                    //google weather
                    Log.d("wear main activity", " GOOGLE package launched");
                    openApp(MainActivity.this, GOOGLE_PACKAGE);
                } else if (s.equals("6")) {
                    //google hangout
                    openApp(MainActivity.this, HANGOUTS_PACKAGE);
                } else if (s.equals("7")) {
                    //google findphone
                    Log.d("wear main activity", " findphone launched");
                    openApp(MainActivity.this, FIND_PHONE_PACKAGE);
                } else if (s.equals("8")) {
                    //maps
                    openApp(MainActivity.this, MAPS_PACKAGE);
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        return scaleAnimator;
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting()))
            mApiClient.connect();

        BlowRecognitionUtil.getInstance(this).setUICallback(new UICallback());
        BlowRecognitionUtil.getInstance(this).startListeningForBlow();
    }


    @Override
    protected void onStop() {
        if (mApiClient != null) {
            Wearable.MessageApi.removeListener(mApiClient, this);
            if (mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        BlowRecognitionUtil.getInstance(this).stopListeningForBlow();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        if (mApiClient != null)
            mApiClient.unregisterConnectionCallbacks(this);
        super.onDestroy();
    }


    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);
        if (i == null) {
            return false;
            //throw new PackageManager.NameNotFoundException();
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(i);
        return true;
    }

    public class UICallback {
        public static final int RESULT_FIELD = 0x001;

        private Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == RESULT_FIELD) {
                    String result = (String) msg.obj;
                    switch (result) {
                        case "no sound":
                            break;
                        case "topleft":
                            handleTopLeft();
                            break;
                        case "topright":
                            handleTopRight();
                            break;
                        case "bottomleft":
                            handleBottomLeft();
                            break;
                        case "bottomright":
                            handleBottomRight();
                            break;
                        default:

                    }
                }
            }
        };

        public Message obtainMessage(String result) {
            return mHandler.obtainMessage(RESULT_FIELD, result);
        }

        private void handleTopLeft() {

            Log.d(TAG, "handling longblow");
            s="3";
            getAnimationForImageView(mkeep).start();

        }

        private void handleTopRight() {
            s="2";
            Log.d(TAG,"handling doubleblow");
            getAnimationForImageView(mWater).start();
        }

        private void handleBottomLeft() {
            s="1";//string indicating the icon number
            Log.d(TAG,"handling shortblow");
            getAnimationForImageView(mHangouts).start();

        }

        private void handleBottomRight() {
            s="1";//string indicating the icon number
            Log.d(TAG,"handling shortblow");
            getAnimationForImageView(mMaps).start();

        }


    }




}
