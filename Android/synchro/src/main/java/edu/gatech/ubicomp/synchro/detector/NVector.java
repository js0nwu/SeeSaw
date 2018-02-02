package edu.gatech.ubicomp.synchro.detector;

import java.util.List;

/**
 * Created by jwpilly on 9/8/16.
 */

public class NVector {
    private float[] vectorData;

    public NVector(float[] vectorData) {
        this.vectorData = vectorData;
    }

    public NVector(List<Float> vectorList) {
        float[] floatArray = new float[vectorList.size()];
        int i = 0;
        for (Float f : vectorList) {
            floatArray[i++] = (f != null ? f : Float.NaN);
        }
        this.vectorData = floatArray;
    }

    public void setVectorData(float[] vectorData) {
        this.vectorData = vectorData;
    }

    public float[] getVectorData() {
        return vectorData;
    }

    public float magnitude() {
        float mag = 0;
        for (int i = 0; i < vectorData.length; i++) {
            mag += vectorData[i] * vectorData[i];
        }
        return (float) Math.sqrt((double) mag);
    }

    public int getDimensions() {
        return vectorData.length;
    }

    public float getValue(int index) {
        return vectorData[index];
    }

    public void setValue(int index, float value) {
        vectorData[index] = value;
    }

    public float distanceFrom(NVector v) {
        if (getDimensions() != v.getDimensions()) {
            throw new IllegalArgumentException("incompatible dimensions: " + getDimensions() + " vs " + v.getDimensions());
        }
        float mag = 0;
        for (int i =0; i < vectorData.length; i++) {
            float diff = v.getValue(i) - vectorData[i];
            mag += diff * diff;
        }
        return (float) Math.sqrt((double) mag);
    }

    public float getMean() {
        float sum = 0;
        for (int i = 0; i < vectorData.length; i++) {
            sum += vectorData[i];
        }
        return sum / vectorData.length;
    }

    public float getVariance() {
        float mean = getMean();
        float tempSum = 0;
        for (int i = 0; i < vectorData.length; i++) {
            tempSum += (vectorData[i] - mean) * (vectorData[i] - mean);
        }
        return tempSum / vectorData.length;
    }

    public float getStd() {
        return (float) Math.sqrt(getVariance());
    }
}
