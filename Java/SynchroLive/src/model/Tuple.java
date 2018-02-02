package model;

/**
 * Pairs each float array with a certain label
 * Useful for Scenario 1, so that float arrays dont get mixed up between threads
 * Created by Victor on 11/21/2016.
 */
public class Tuple {

    private float[] input;
    private String trainingLabel;

    public Tuple(float[] input, String trainingLabel) {
        this.input = input;
        this.trainingLabel = trainingLabel;
    }

    public float[] getInput() {
        return input;
    }

    public String getTrainingLabel() {
        return trainingLabel;
    }
}
