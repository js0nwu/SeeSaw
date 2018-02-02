package classification;

import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Victor on 11/7/2016.
 */
public class Classifier extends Observable implements Observer, Runnable {

    SMO cls = new SMO();

    PolyKernel poly = new PolyKernel();

    Instances arffTrainingInstances;

    Instances arffTestInstances;

    String[] classLabels;

    BlockingQueue<Instance> instanceQueue = new LinkedBlockingQueue<>();

    boolean isDone = false;

    public Classifier(String[] classLabels) {
        this.classLabels = new String[classLabels.length];
        for (int i = 0; i < classLabels.length; i++) {
            this.classLabels[i] = classLabels[i];
        }
    }

    @Override
    public void update(Observable obj, Object arg) {
        if (arg instanceof Instance) {
            instanceQueue.add((Instance)arg);
        } else if (arg instanceof String) {
            if (((String)arg).equals("Done calculating")) {
                isDone = true;
            }
        } else {
            System.out.println("Classifier did not get an Instance");
        }
    }

    @Override
    public void run() {
        while (!isDone || !instanceQueue.isEmpty()) {
            if (!instanceQueue.isEmpty()) {
                try {
                    arffTestInstances.add(instanceQueue.take());
                    String result = classifyGesture();
                    setChanged();
                    notifyObservers(result);


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Method to build Weka classifier
     */
    public void buildClassifier() {
        try
        {
            // Set classifier options (do not normalize or standardize)
            System.out.println(Arrays.toString(cls.getOptions()));
            String[] options = new String[2];
            options[0] = "-N";
            options[1] = "2";
            cls.setOptions(options);

            // Set exponent to cubic polynomial
            poly.setExponent(1.0);

            // Set kernel for classifier to use
            cls.setKernel(poly);

            // Build classifier
            cls.buildClassifier(arffTrainingInstances);

            // Debug
            System.out.println(Arrays.toString(cls.getOptions()));
            System.out.println("Classifier built using training instances");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Method to classify current window of data (last instance in the test set)
     */
    public String classifyGesture()
    {
        double result = 0;

        if (arffTestInstances.numInstances() >= 1)
        {
            try
            {
                // Classify the instance
                result = cls.classifyInstance(arffTestInstances.lastInstance());

                // Get the result class label and send it back to the Pipeline
                final String resultLabel = classLabels[(int)result];
                return resultLabel;
                // Output label
//                System.out.println(resultLabel);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("No instances to classify");
        }
        return null;
    }

    /**
     * Method to load arff file
     */
    public void loadArffFile(InputStream inputStream)
    {
        ArffLoader loader = new ArffLoader();

        try
        {
            loader.setSource(inputStream);
//            System.out.println(arffTrainingInstances.size());
            arffTrainingInstances = loader.getStructure();
            arffTestInstances = loader.getStructure();
            arffTrainingInstances = loader.getDataSet();
            arffTrainingInstances.setClassIndex(arffTrainingInstances.numAttributes() - 1);
            arffTestInstances.setClassIndex(arffTrainingInstances.numAttributes() - 1);
            System.out.println(arffTrainingInstances.size());
            //System.out.println(arffTrainingInstances.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("GestureClassification: no model file exists");
        }
    }


}
