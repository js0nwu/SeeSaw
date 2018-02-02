package edu.gatech.ubicomp.synchro.detector;

/**
 * Created by jwpilly on 2/16/17.
 */
public class CountSubsampler implements Subsampler {
    private double[] timeArray;
    private double[][] sampleArray;
    private int bufferPointer = 0;

    public CountSubsampler(int size) {
        timeArray = new double[size];
        sampleArray = new double[size][];
    }

    public Tuple2<double[]> processSample(double time, double[] sample) {
        timeArray[bufferPointer] = time;
        sampleArray[bufferPointer] = sample;
        if (bufferPointer == sampleArray.length - 1) {
            double[] averageSample = new double[sampleArray[0].length];
            for (int i = 0; i < sampleArray.length; i++) {
                for (int j = 0; j < averageSample.length; j++) {
                    averageSample[j] += sampleArray[i][j];
                }
            }
            for (int i = 0; i < averageSample.length; i++) {
                averageSample[i] /= bufferPointer + 1;
            }
            double averageTime = 0;
            for (int i = 0; i < timeArray.length; i++) {
                averageTime += timeArray[i];
            }
            averageTime /= timeArray.length;
            bufferPointer = 0;
            return new Tuple2<>(new double[] {averageTime}, averageSample);
        } else {
            bufferPointer++;
            return null;
        }
    }
}
