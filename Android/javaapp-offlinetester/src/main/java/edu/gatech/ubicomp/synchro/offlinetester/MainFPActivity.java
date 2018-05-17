package edu.gatech.ubicomp.synchro.offlinetester;

import com.opencsv.CSVReader;
import edu.gatech.ubicomp.synchro.detector.EventRecognitionListener;
import edu.gatech.ubicomp.synchro.detector.SynchroDetector;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static edu.gatech.ubicomp.synchro.detector.Utils.logOut;

/**
 * Created by jwpilly on 1/30/17.
 */
public class MainFPActivity {

	private static String TAG = "MainFPActivity";

	private static double SYNC_LOCK = 2000;
	private static double lastSync = 0;

	private static boolean debugMode = false;

	private static double syncThreshold = 1.0F;
	private static int triggerCount = 0;

	private static long startTime = 0;
	private static long endTime = 0;

	private static boolean activationMode = false;
	private static boolean correlationMode = false;

	private static String outputString = "";

	public static void main(String[] args) {
		if (args.length != 2 && args.length != 3) {
			return;
		}
		if (args.length >= 3) {
			if (args[2].equals("1")) {
				activationMode = true;
			}
		}
		String filename = args[0];
		if (args[1].equals("-1")) {
			correlationMode = true;
		}
		syncThreshold = Float.parseFloat(args[1]);
		calculateCorrelation(Paths.get(filename));
	}

	private static void calculateCorrelation(Path file) {

		SynchroDetector syncDetector = new SynchroDetector(false);
		syncDetector.setActivationMode(activationMode);
		syncDetector.setEventRecognitionListener(new EventRecognitionListener() {
			@Override
			public void onEventRecognized(String result) {
				if(debugMode) logOut(TAG, result);
				if (!correlationMode && result != null && result.contains(",")) {
					String[] row = result.split(",");
					double timestamp = Double.parseDouble(row[0]);
					double correlation = Double.parseDouble(row[1]);
					if (correlation >= syncThreshold && (timestamp - lastSync > SYNC_LOCK)) {
						lastSync = timestamp;
						triggerCount++;
					}
				} else {
					outputString += result + "\n";
				}
			}
		});
		String inputFilePath = file.toString();
		String inputFileName = file.getFileName().toString();
		String outputBase = inputFileName.substring(0, inputFileName.length() - 4);

		double lastConvertedTS = 0;
		int referencePeriod = 1000;
		syncDetector.referencePeriod = referencePeriod;

		boolean begun = true;

		try {
			File inputFileRead = new File(inputFilePath);
			Scanner sc = new Scanner(inputFileRead);
			StringBuilder sb = new StringBuilder();
			while (sc.hasNextLine()) {
				sb.append(sc.nextLine());
			}
			sc.close();
			String fileContents = sb.toString();
			if (fileContents.contains("begin")) {
				begun = false;
			}
			CSVReader reader = new CSVReader(new FileReader(inputFilePath));
			List<String[]> csvContents = reader.readAll();
			for (String[] row : csvContents) {
			    if (row.length < 2) {
			    	continue;
				}
				if (row[1].contains("begin")) {
					begun = true;
					startTime = Long.parseLong(row[0]);
				}
				if (row[1].contains("end")) {
			    	begun = false;
			    	endTime = Long.parseLong(row[0]);
				}
				if (!begun) {
					continue;
				}
				if (row.length < 2) {
					continue;
				}
				if (row[1].equals("top")) {
					syncDetector.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
				} else if (row[1].equals("bottom")) {
					syncDetector.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
				}
				// new user study data
				if (row.length == 8) {
					if (row[1].equals(Config.SENSOR_NAME)) {
						double sensorTimestamp = Double.parseDouble(row[6]);
						lastConvertedTS = sensorTimestamp / 1000000;
						double[] windowTime = new double[]{lastConvertedTS};
						double[] windowData = new double[3];
						windowData[0] = Double.parseDouble(row[2]);
						windowData[1] = Double.parseDouble(row[3]);
						windowData[2] = Double.parseDouble(row[4]);
						Tuple2 windowTuple = new Tuple2(windowTime, windowData);
						syncDetector.dataBuffer.offer(windowTuple);
					}
				}
			}

			syncDetector.run();

			syncDetector.isRunning = false;
		} catch (Exception e) {
			System.out.println("error while processing file");
			e.printStackTrace();
		} finally {
		    if (correlationMode) {
		    	System.out.println(outputString);
			} else {
				System.out.println("" + triggerCount + "," + (endTime - startTime));
			}
		}
	}

	private static void writeMapToFile(Map<?,?> inputMap, String fileName) {
		if (!fileName.equals("")) {
			try {
				File writeFile = new File(fileName);
				if (writeFile.exists()) {
					writeFile.delete();
				}
				PrintWriter writer = new PrintWriter(fileName, "UTF-8");
				for (Object key : inputMap.keySet()) {
					String row = key.toString() + "," + inputMap.get(key);
					writer.println(row);
				}
				writer.close();
			} catch (IOException e) {
				System.out.println("error while writing map to file");
				e.printStackTrace();
			}
		} else {
			logOut(TAG, "filename is null");
		}
	}

}
