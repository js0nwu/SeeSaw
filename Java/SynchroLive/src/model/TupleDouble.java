package model;

/**
 * Pairs each double array with a certain label
 * Useful for Scenario 1, so that float arrays dont get mixed up between threads
 * Created by Victor on 11/25/2016.
 */
public class TupleDouble {
    private double[] input;
    private String trainingLabel;

    public TupleDouble(double[] input, String trainingLabel) {
        this.input = input;
        this.trainingLabel = trainingLabel;
    }

    public double[] getInput() {
        return input;
    }

    public String getTrainingLabel() {
        return trainingLabel;
    }

}
