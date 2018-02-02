package com.sarthakghosh.wearapp_shortcuts;

import android.content.Context;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import edu.gatech.ubicomp.whoosh.recognizer.AudioService;
import edu.gatech.ubicomp.whoosh.recognizer.SimpleRecognizer;
import edu.gatech.ubicomp.whoosh.recognizer.TrainedRecognizer;

/**
 * Created by batman on 7/4/16.
 */
public class BlowRecognitionUtil implements TrainedRecognizer.EventRecognitionListener {
    public static final String TAG = BlowRecognitionUtil.class.getSimpleName();

    private static BlowRecognitionUtil mBlowRecognitionUtil = null;
    private TrainedRecognizer mTrainedRecognizer;

    private AudioProcessor mAudioProcessor = new AudioProcessor() {
        @Override
        public boolean process(AudioEvent audioEvent) {
            mTrainedRecognizer.recognizeAudioBuffer(audioEvent);
            return true;
        }

        @Override
        public void processingFinished() {

        }
    };

    private boolean mIsListeningForBlow = false;
    private MainActivity.UICallback mUICallback;

    private BlowRecognitionUtil(Context context) {
        InputStream inputStream = null;
        try {
//				Log.d(TAG, "" + assetManager.getLocales());
            inputStream = context.getAssets().open("arff/ShortcutCase.arff");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTrainedRecognizer = new TrainedRecognizer(inputStream, "MFCC", true);
        mTrainedRecognizer.setEventRecognitionListener(this);
    }

    public static BlowRecognitionUtil getInstance(Context context) {
        if (mBlowRecognitionUtil == null) {
            mBlowRecognitionUtil = new BlowRecognitionUtil(context);
        }

        return mBlowRecognitionUtil;
    }


    @Override
    public void onEventRecognized(String result) {
        Message message = mUICallback.obtainMessage(result);
        message.sendToTarget();
    }

    public void setUICallback(MainActivity.UICallback callback) {
        mUICallback = callback;
    }

    public void startListeningForBlow() {
        if (!mIsListeningForBlow) {
            mIsListeningForBlow = true;
            AudioService.getInstance(false).startThread(mAudioProcessor);
        }
    }

    public void stopListeningForBlow() {
        if (mIsListeningForBlow) {
            mIsListeningForBlow = false;
            AudioService.getInstance(false).stopThread(mAudioProcessor);
        }
    }

    public boolean getIsListeningForBlow() {
        return mIsListeningForBlow;
    }
}
