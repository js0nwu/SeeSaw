package edu.gatech.ubicomp.synchro.livedatacollect;

/**
 * Created by jwpilly on 9/16/2016.
 */
public class Experiment {
    private String person;
    private String experiment;
    private int flashes;
    private int ticks;
    private String scenario;

    public Experiment(String person, String experiment, int flashes, int ticks, String scenario) {
        this.person = person;
        this.experiment = experiment;
        this.flashes = flashes;
        this.ticks = ticks;
        this.scenario = scenario;
    }

    public String getPerson() {
        return person;
    }

    public String getExperiment() {
        return experiment;
    }

    public int getFlashes() {
        return flashes;
    }

    public int getTicks() {
        return ticks;
    }

    public String getScenario() {
        return scenario;
    }

    @Override
    public String toString() {
        return "person: " + person + " experiment: " + experiment + " flashFreq: " + flashes + " numOfCycles: " + ticks + " scenario: " + scenario;
    }
}
