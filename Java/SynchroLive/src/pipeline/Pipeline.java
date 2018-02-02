package pipeline;

import classification.Classifier;
import crossValidation.CrossValidater;
import featureExtractor.FeatureExtractor;
import model.Command;
import model.Tuple;
import model.TupleDouble;
import preprocessing.PreProcessor;
import segmentation.Segmenter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Machine learning pipeline
 *
 * IMPORTANT: When creating JAR, see note in CrossValidater
 *            -Also, when compiling to use with Android, you MUST switch the Weka JAR file to the weka_STRIPPED
 *            JAR file to work with Android, otherwise use the normal weka jar (when running scenario 1 or 2)
 *            -Reason being Android does not like some of the Java stuff in original Weka, but CrossValidater
 *            uses some of those classes that Android doesn't like
 *
 * Created by Victor on 11/3/2016.
 */
public class Pipeline extends Observable implements Observer, Runnable {

    private PreProcessor myPreProcessor;
    private Classifier myClassifier;

    ArrayList<Attribute> arffAttributes;
    ArrayList<String> arffClassLabels;
    Instances arffTrainingInstances;

    List<Segmenter> segmentBranches;
    List<FeatureExtractor> featureBranches;
    Set<String> branchNames;

    BlockingQueue<TupleDouble> featureQueue;
    private boolean isDone = false;

    //meta data
    private String segmentationType;
    private int bufferSize;
    private int overlap;
    private String featureType;
    private String[] classLabels;
    private int featureNum;
    private String arffPath = "";
    private ArffSaver saver = new ArffSaver();
    private CSVSaver saver1 = new CSVSaver();
    private Command onResult;
    private String resultLabel;

    private int sampleRate;
    private int mfccBufferSize;
    private int cepstrumCoefSize;
    private int melFilterSize;
    private float lowerFilterFreq;
    private float upperFilterFreq;

    /**
     * NOTE: may want to switch this so that scenario is passed into Pipeline constructor
     *
     * Scenario 1 = train offline, cross validate offline
     * Scenario 2 = load offline model, test with offline data
     * Scenario 3 = load offline model, test with live data
     * Scenario 4 = train live, test live*
     */
    private final int scenario = 1;


    /**
     * Constructor for pipeline of offline training
     * @param segmentationType "sliding" window or "event" segmentation
     * @param bufferSize size of frame/window
     * @param featureType for audio data: mfcc or fft
     * @param classLabels labels for arff generation
     * @param onResult the method to execute when Classifier returns a classification label
     */
    public Pipeline(String segmentationType, int bufferSize, int overlap, String featureType, String[] classLabels,
                    String arffFileLoc, Command onResult) {
        this.classLabels = new String[classLabels.length];
        for (int i = 0; i < classLabels.length; i++) {
            this.classLabels[i] = classLabels[i];
        }
        this.segmentationType = segmentationType;
        this.bufferSize = bufferSize;
        this.overlap = overlap;
        this.featureType = featureType;
        this.onResult = onResult;
        branchNames = new HashSet<>();

        featureQueue = new LinkedBlockingQueue<>();

        if (segmentationType == "sliding") {
            featureNum = 26;
        } else if (segmentationType == "event") {
            featureNum = 78;
        }

        segmentBranches = new ArrayList<>();
        featureBranches = new ArrayList<>();

        arffAttributes = new ArrayList<Attribute>();
        arffClassLabels = new ArrayList<String>(Arrays.asList(classLabels));

        setArffAttributesHeaders(featureType, featureNum, arffClassLabels);

        arffTrainingInstances = new Instances("GestureTraining", arffAttributes, 0);

        arffTrainingInstances.setClassIndex(arffTrainingInstances.numAttributes() - 1);

        setArffFilePath(arffFileLoc);

        if (scenario == 1) {
            CrossValidater cv = new CrossValidater();
            this.addObserver(cv);
        } else if (scenario > 1) {
            try {
                Classifier myClassifier = new Classifier(classLabels);
                this.addObserver(myClassifier);
                myClassifier.addObserver(this);

                InputStream input = new FileInputStream(arffPath + "/training.arff");
                myClassifier.loadArffFile(input);
                myClassifier.buildClassifier();

                new Thread(myClassifier){}.start();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        new Thread(this){}.start();
    }

    @Override
    public void update(Observable obj, Object arg) {
        //Receives data from FeatureExtractors as TupleDoubles
        if (arg instanceof TupleDouble) {
            featureQueue.add((TupleDouble) arg);
        } else if (arg instanceof String) {
            //Receieved classifications as Strings from the Classifier
            resultLabel = (String) arg;
            onResult.execute(resultLabel);
        }
    }

    @Override
    public void run() {
        while (!isDone) {
            if (!featureQueue.isEmpty()) {
                try {
                    //Take the array from the FeatureExtractors and instantiate it in the model
                    TupleDouble temp = featureQueue.take();
                    instantiateFeatures(temp.getInput(), temp.getTrainingLabel());

                    if (scenario > 1) { //pass on the Instance to the Classifier to classify
                        setChanged();
                        notifyObservers(arffTrainingInstances.lastInstance());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Must use this method at least once when using the Pipeline.
     * Creates a separate "branch" for each set of data that the pipeline gets
     *      i.e. In scenario 1, with multiple files, create "branches" for each file read in
     *           In scenario 3, since using live data (one continuous set of data) use createBranch() once
     * Each "branch" represents 1 Segmenter and 1 FeatureExtractor attached to the Segmenter
     * This is so all segmentation and feature extraction can be run on their own threads and data is not mixed up
     * @param trainingLabel the class label
     *                  !!!!IMPORTANT: trainingLabel here must be the same as trainingLabel used in read(),
     *                      NOTE: Above is especially important in scenario 3 when using live data.
     *                          Can fix this by passing scenario to Segmenters and Extractors
     */
    public void createBranch(String trainingLabel) {
        if (!branchNames.contains(trainingLabel)) {
            Segmenter mySegmenter = new Segmenter(segmentationType, bufferSize, overlap, trainingLabel);
            FeatureExtractor myFeatureExtractor = null;
            branchNames.add(trainingLabel);

            if (segmentationType == "sliding") {
                myFeatureExtractor = new FeatureExtractor(featureType, segmentationType, trainingLabel);
            } else if (segmentationType == "event") {
                myFeatureExtractor = new FeatureExtractor(featureType, segmentationType, trainingLabel);
            }


            if (mySegmenter != null && myFeatureExtractor != null) {
                myFeatureExtractor.setMFCC(mfccBufferSize, sampleRate, cepstrumCoefSize, melFilterSize, lowerFilterFreq, upperFilterFreq);

                this.addObserver(mySegmenter);
                mySegmenter.addObserver(myFeatureExtractor);
                myFeatureExtractor.addObserver(this);
            }

            segmentBranches.add(mySegmenter);
            featureBranches.add(myFeatureExtractor);

            new Thread(mySegmenter) {
            }.start();
            new Thread(myFeatureExtractor) {
            }.start();
        }
    }

    /**
     * Method for passing data to Pipeline
     * @param input float array with data to process
     * @param trainingLabel class label for training
     *                  !!!!IMPORTANT: see createBranch()
     */
    public void read(float[] input, String trainingLabel) {
        setChanged();
        float[] temp = input.clone();
        notifyObservers(new Tuple(temp, trainingLabel));
    }

    /**
     * For Scenario 1
     * Called after Pipeline finishes reading in data aka after all files have finished being read
     * Will eventually stop Pipeline main thread
     */
    public void finish() {
        // tell FeatureExtractors the pipeline will get no more new data
        for (FeatureExtractor f : featureBranches) {
            f.flag();
        }
        //tell Segmenters that pipeline will get no more new data
        setChanged();
        notifyObservers("pip is done");
        //Wait until all FeatureExtractors attached to Pipeline have finished calculating
        while (!isDone) {
            int count = 0;
            for(FeatureExtractor f : featureBranches) {
                //check if the FeatureExtractors are done calculating
                if(f.checkFinished()) {
                    count++;
                }
            }
            //For Scenario 1
            // When all FeatureExtractors are done calculating and Pipeline has finished putting features into model
            // then save the arff file
            if(count == featureBranches.size() && featureQueue.isEmpty()) {
                if (scenario == 1) {
                    saveArffFile(arffTrainingInstances, "offlineTraining");
                }
                isDone = true;
                setChanged();
                notifyObservers("Done calculating");
            }
        }
        //Send the model to the CrossValiddater
        if (scenario == 1) {
            InputStream inputStream = null;
            System.out.println("SENDING STUFF TO THE CROSS VALIDATER" + isDone);
            try {
                inputStream = new FileInputStream("C:/MLearning/arfffiles/training.arff");
                System.out.println("Loaded the arff file that was just created");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            setChanged();
            notifyObservers(inputStream); //send to the cross validater
        }
    }

    /**
     * When selected feature to calculate is MFCC, use method to configure the MFCC for feature extraction
     * @param bufferSize a
     * @param sampleRate a
     * @param cepstrumCoefSize a
     * @param melFilterSize a
     * @param lowerFilterFreq a
     * @param upperFilterFreq a
     */
    public void setMFCC(int bufferSize, int sampleRate, int cepstrumCoefSize, int melFilterSize, float lowerFilterFreq, float upperFilterFreq) {
        this.mfccBufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.cepstrumCoefSize = cepstrumCoefSize;
        this.melFilterSize = melFilterSize;
        this.lowerFilterFreq = lowerFilterFreq;
        this.upperFilterFreq = upperFilterFreq;
    }

    /**
     * Method to create the headers for the instance file
     */
    private void setArffAttributesHeaders(String featureName, int featureNum, ArrayList<String> classLabels) {
        if(featureName.equals("FFT"))
        {

            for (int i = 0; i < featureNum; i++)
            {
                arffAttributes.add(new Attribute("freqaml_" + i));
            }

        }
        else if(featureName.equals("MFCC"))
        {
            for (int i = 0; i < featureNum; i++)
            {
                arffAttributes.add(new Attribute("MFCC_" + i));
            }
        }

        // Add the class labels
        arffAttributes.add(new Attribute("@@class@@", classLabels));
    }


    /**
     * Method to save instances to arff file
     */
    private void saveArffFile(Instances instances, String instancesType)
    {
        try
        {
//            System.out.println(instances.size());
            saver.setInstances(instances);
            saver.writeBatch();
            saver1.setInstances(instances);
            saver1.writeBatch();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //System.out.println("GestureClassification: exception saving the arff file");
        }
    }

    /**
     * Method to set the file path of where the arff will be located
     * @param path String
     */
    private void setArffFilePath(String path) {
        this.arffPath = path;

        if (scenario == 1) {
            String fileName = arffPath + "/training" + ".arff";
            try {
                saver.setFile(new File(fileName));
                saver1.setFile(new File(arffPath + "/training.csv"));

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to store instances of training data
     */
    public void instantiateFeatures(double[] featureVector, String trainingLabel) {

        // Pass features into double[]
        double[] featureValues = new double[arffAttributes.size()];
        for (int i = 0; i < featureVector.length; i++)
            featureValues[i] = featureVector[i];
        featureValues[featureVector.length] = (double) arffTrainingInstances.classAttribute().indexOfValue(trainingLabel);
        // New instance with current feature values and label
        DenseInstance newInstance = new DenseInstance(1.0, featureValues);

        // Add new instance to dataset
        arffTrainingInstances.add(newInstance);
//        System.out.println("ASDFASDFASDF" + arffTrainingInstances.size());

        // Save new instances object file
//            saveArffFile(arffTrainingInstances, "training");
    }

}
