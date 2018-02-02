package featureExtractor;

import be.tarsos.dsp.mfcc.MFCC;
import model.TupleDouble;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Calculates features on a segmented window of data
 * Created by Victor on 11/3/2016.
 */
public class FeatureExtractor extends Observable implements Observer, Runnable {

    private MFCC mfcc;

    private String featureType;
    private List<float[]> eventData;
    private String segmentation;
    private String trainingLabel;


    private float[] receivedData;
    private BlockingQueue<float[]> dataBuffer;

    private boolean isDoneReceiving = false;
    private boolean isDoneSegmenting = false;
    private boolean isFinished = false;
    private int count = 0;

    public FeatureExtractor(String featureType, String segmentation, String trainingLabel) {
        System.out.println("Feature Extractor was created");
        this.featureType = featureType;
        this.segmentation = segmentation;
        this.trainingLabel = trainingLabel;

        receivedData = null;

        eventData = new ArrayList<>();
        dataBuffer = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {

        boolean go = true;
        while (go) {
            if (!dataBuffer.isEmpty()) {
                System.out.println("DATA IS NOT NULLLLLL" + dataBuffer.size());
                try {
                    //gets a frame of data from the queue, converts to a double[], depending on feature
                    receivedData = dataBuffer.take().clone();

                    double[] receivedDoubles = new double[receivedData.length];
                    for (int i = 0; i < receivedDoubles.length; i++) {
                        receivedDoubles[i] = receivedData[i];
                    }

                    if (featureType == "MFCC") {
                        if (segmentation == "sliding") {
                            double[] temp = calculateMFCC(receivedData);
                            count++; //for debugging
                            setChanged();
                            notifyObservers(new TupleDouble(temp, trainingLabel));
                        } else { //event segmentation
                            //Collect all incoming frames to process as a whole event
                            eventData.add(receivedData);
                        }
                    } else if (featureType == "RMS") {
                        if (segmentation == "sliding") {
                            double [] temp = calculateRMS(receivedDoubles);
                            count++;
                            setChanged();
                            notifyObservers(new TupleDouble(temp, trainingLabel));
                        } else {
                            //TODO: event segmentation
                        }
                    } else if (featureType == "STD") {
                        if (segmentation == "sliding") {
                            double[] temp = calculateStDev(receivedDoubles);
                            count++;
                            setChanged();
                            notifyObservers(new TupleDouble(temp, trainingLabel));
                        } else {
                            //TODO: event segmentation
                        }
                    } else if (featureType == "mean") {
                        if (segmentation == "sliding") {
                            double[] temp = calculateMean(receivedDoubles);
                            count++;
                            setChanged();
                            notifyObservers(new TupleDouble(temp, trainingLabel));
                        } else {
                            //TODO: event segmentation
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //when Pipeline stops getting data and corresponding Segmenter finishes
                if (isDoneReceiving && isDoneSegmenting) {
                    go = false;
                    finish();
                    isFinished = true;
                    System.out.println(count + "$$$$$$$$$" + trainingLabel);
                }


            }
        }
    }

    /**
     * Method to calculate the MFCC vector for a window of data
     * @param dataArray
     * @return double[] the vector
     */
    public double[] calculateMFCC(float[] dataArray) {
        //Calculate the 26 MFCCs for the incoming frame
        //Magnitude Spectrum
        float[] bin = mfcc.magnitudeSpectrum(dataArray);
        //get Mel Filter Bank
        float[] fbank = mfcc.melFilter(bin, mfcc.getCenterFrequencies());
        //get Non-Linear Transformation
        float[] f = mfcc.nonLinearTransformation(fbank);
        float[] features = mfcc.cepCoefficients(f);
        double[] temp = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            temp[i] = features[i];
        }
        return temp;
    }

    /**
     * Method to calculate the RMS for given window of data
     * @param dataArray
     * @return
     */
    public double[] calculateRMS(double[] dataArray)
    {
        double result = 0;
        int chLen = dataArray.length;

        for (int j = 0; j < chLen; j++)
        {
            result += dataArray[j] * dataArray[j];
        }
        double[] calcValues = new double[1];
        calcValues[0] = Math.sqrt(result / chLen);
        return calcValues;
    }

    /**
     * Method to calculate the standard deviation for given window of data
     * @param dataArray
     * @return
     */
    public double[] calculateStDev(double[] dataArray)
    {
        double result = 0;
        result = StatUtils.variance(dataArray);
        double[] calcValues = new double[1];
        calcValues[0] = Math.sqrt(result);
        return calcValues;
    }

    /**
     * Method to calculate the mean for given window of data
     * @param dataArray
     * @return
     */
    public double[] calculateMean(double[] dataArray)
    {
        double result = 0;
        result = StatUtils.mean(dataArray);
        double[] calcValues = new double[1];
        calcValues[0] = result;
        return calcValues;
    }

    @Override
    public void update(Observable obj, Object arg) {
        //if the Segmenter passes a float[] of new data
        if (arg instanceof float[]) {
//            System.out.println("FeatureExtractor got some data");
            float[] tempBuff = ((float[])arg).clone();

            dataBuffer.offer(tempBuff);
        } else if (arg instanceof String) {
            //Segmenter tells FeatureExtractor that it's done segmenting
            isDoneSegmenting = true;
        } else {
//            System.out.println("Received object is invalid for FeatureExtractor");
        }
    }

    public void flag() {
        isDoneReceiving = true;
    }

    /**
     * Method called when FeatureExtractor finishes all calculations
     * If generating MFCC vectors with event segmentation, features are calculated at the end,
     * after all data is collected.
     */
    private void finish() {
        if (featureType == "MFCC") {
            if (segmentation == "event") {
                double[] finalFeatures = calculateFinalFeatures();
                setChanged();
                notifyObservers(new TupleDouble(finalFeatures, trainingLabel));
            }
        } else {
            System.out.println("not doing MFCC");
        }
    }

    /**
     * Method for configuring FeatureExtractor if calculating MFCC's is necessary. Uses TarsosDSP MFCC calculations
     * @param bufferSize size of frame/buffer
     * @param sampleRate sample rate of auduio
     * @param cepstrumCoefSize a
     * @param melFilterSize a
     * @param lowerFilterFreq a
     * @param upperFilterFreq a
     */
    public void setMFCC(int bufferSize, int sampleRate, int cepstrumCoefSize, int melFilterSize, float lowerFilterFreq, float upperFilterFreq) {
        mfcc = new MFCC(bufferSize, sampleRate, cepstrumCoefSize, melFilterSize, lowerFilterFreq, upperFilterFreq);
    }


    /**
     * Method used when doing event segmentation. Used in MFCC feature calculations in Whoosh
     * aka calculates features forward (26), backwards (26), and on deltas (26)
     * @return double[] the features
     */
    private double[] calculateFinalFeatures() {
        double[] featureVector = {};
        int featureSize = 0;
//        if (featureName == "FFT") {
//            featureSize = Config.FFT_SIZE ;
//
//        } else if(featureName == "MFCC"){
        featureSize = 26;
//        }


        featureVector = new double[featureSize * 3];
//        double[] max = {-100, -100};
        for(int i = 0; i < eventData.size() / 2; i++)
        {
            for(int j = 0; j < featureSize; j++)
            {
                featureVector[j] += eventData.get(i)[j];

//                if(max[0] < event.get(i)[j])
//                	max[0] = event.get(i)[j];
            }
        }
        for(int i = eventData.size() / 2; i < eventData.size(); i++)
        {
            for(int j = 0; j < featureSize; j++)
            {
                featureVector[featureSize + j] += eventData.get(i)[j];
//                if(max[0] < event.get(i)[j])
//                	max[0] = event.get(i)[j];
            }
        }
//        for(int i = 0; i < featureSize * 2; i++)
//        	featureVector[i] = featureVector[i] / max[0];

        for(int i = 0; i < featureSize; i++)
        {
            featureVector[featureSize*2 + i] = featureVector[i] - featureVector[featureSize+i];
//        	if(max[1] < featureVector[featureSize*2 + i])
//            	max[1] = featureVector[featureSize*2 + i];
        }


        //System.out.println(featureVector.length);
        //System.out.println("feature calculated");

        return featureVector;
    }

    /**
     * Used by Pipeline to check if the FeatureExtractor has finished doing all calculations.
     * Relevant for Scenario 1, when the pipeline reads in data from files much quicker than
     * features are calculated.
     * @return
     */
    public boolean checkFinished() {
        return isFinished;
    }

}
