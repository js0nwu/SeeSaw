package model;

/**
 * Represents the method to be executed when Classifier returns the classLabel
 * Created by Victor on 12/6/2016.
 */
public interface Command {

    void execute(String s);
}
