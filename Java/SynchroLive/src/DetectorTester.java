import classification.Synchro.SynchroDetector;
import com.opencsv.CSVReader;
import util.Tuple2;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jwpilly on 1/30/17.
 */
public class DetectorTester {

    public static void main(String[] args) {
        String csvPath = args[0];
        int windowSize = Integer.parseInt(args[1]);
        int referencePeriod = Integer.parseInt(args[2]);
        int generatePeriod = Integer.parseInt(args[3]);
        String outputFile = args[4];
        double firstTimeStamp = -1;
        double firstSensorTimeStamp = 0;
        double lastConvertedTS = 0;
        SynchroDetector sd = new SynchroDetector();
        sd.windowSize = windowSize;
        sd.referencePeriod = referencePeriod;
        sd.generatePeriod = generatePeriod;
        try {
            List<String> rows = new ArrayList<>();
            CSVReader reader = new CSVReader(new FileReader(csvPath));
            List<String[]> csvContents = reader.readAll();
            for (String[] row : csvContents) {
                if (row[1].equals("magnet")) {
                    double timestamp = Double.parseDouble(row[0]);
                    double sensorTimestamp = Double.parseDouble(row[5]);
                    lastConvertedTS = timestamp;
                    if (firstTimeStamp == -1) {
                        firstTimeStamp = 0;
                        firstSensorTimeStamp = sensorTimestamp;
                        timestamp = firstTimeStamp;
                    } else {
                        timestamp = firstTimeStamp + (sensorTimestamp - firstSensorTimeStamp) / 1000000;
                    }
//                    System.out.println("timestamp: " + timestamp);
                    double[] windowTime = new double[] {timestamp};
                    double[] windowData = new double[3];
//                    double elapsedTime = (timestamp - firstTimeStamp) / 1000;
                    windowData[0] = Double.parseDouble(row[2]);
                    windowData[1] = Double.parseDouble(row[3]);
                    windowData[2] = Double.parseDouble(row[4]);
                    Tuple2 windowTuple = new Tuple2(windowTime, windowData);
                    double syncValue = sd.detectValue(windowTuple);
//                    System.out.println("" + elapsedTime + "," + syncValue);
//                    System.out.println("" + timestamp + "," + syncValue);
                    rows.add("" + timestamp + "," + syncValue);
                } else if (row[1].equals("left")) {
                    double timestamp = lastConvertedTS;
                    sd.syncEvent(timestamp, true);
                } else if (row[1].equals("right")) {
                    double timestamp = lastConvertedTS;
                    sd.syncEvent(timestamp, false);
                }
            }
            if (!outputFile.equals("")) {
                String filename = outputFile;
                try{
                    File writeFile = new File(filename);
                    if (writeFile.exists()) {
                        writeFile.delete();
                    }
                    PrintWriter writer = new PrintWriter(filename, "UTF-8");
                    for (String row : rows) {
                        writer.println(row);
                    }
                    writer.close();
                } catch (IOException e) {
                    // do something
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
