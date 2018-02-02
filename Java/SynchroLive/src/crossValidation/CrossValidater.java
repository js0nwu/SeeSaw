package crossValidation;

//If compiling to JAR to use with Android, comment out line below
import weka.classifiers.evaluation.Evaluation;

import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 * Used for offline cross validation
 * NOTE: If building jar file to use on Android, must comment out lines using weka Evaluation class
 *      Android doesn't like it
 * Created by Victor on 11/17/2016.
 */
public class CrossValidater implements Observer {

    public CrossValidater() {

    }

    @Override
    public void update(Observable obj, Object arg) {
        if (arg instanceof InputStream) {
            InputStream input = (InputStream) arg;

            ArffLoader loader = new ArffLoader();

            try
            {
                PolyKernel poly = new PolyKernel();
                loader.setSource(input);
                Instances data = loader.getDataSet();
                data.setClassIndex(data.numAttributes() - 1);
                SMO cls = new SMO();
                String[] options = new String[2];
                options[0] = "-N";
                options[1] = "0";
                cls.setOptions(options);
                // Set exponent to cubic polynomial
                poly.setExponent(1.0);
                // Set kernel for classifier to use
                cls.setKernel(poly);
                Evaluation eval = new Evaluation(data);
                eval.crossValidateModel(cls, data, 10, new Random(1));
                System.out.print("placeholder");
                System.out.print(",");
                System.out.print(Double.toString(eval.pctCorrect()));
                System.out.println();
                System.out.println(eval.toMatrixString());

            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("GestureClassification: no model file exists");
            }
            catch (Exception ee) {
                ee.printStackTrace();
            }
        } else {
            System.out.println("Received object is not an InputStream");
        }
    }

}
