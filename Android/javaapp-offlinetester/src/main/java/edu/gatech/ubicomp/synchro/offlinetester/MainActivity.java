package edu.gatech.ubicomp.synchro.offlinetester;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import edu.gatech.ubicomp.synchro.detector.EventRecognitionListener;
import edu.gatech.ubicomp.synchro.detector.SynchroDetector;
import edu.gatech.ubicomp.synchro.detector.Tuple2;

import static edu.gatech.ubicomp.synchro.detector.Utils.logOut;

/**
 * Created by jwpilly on 1/30/17.
 */
public class MainActivity {

	private static String TAG = "MainActivity";

	private static boolean debugMode = false;

	private static String dataPath;
	private static String outputPath;
	private static String windowPath;
	private static String inputFolder;

	private static HashMap<String, String> usernameRootFolderMap;

	public static void main(String[] args) {

		// File set up
		usernameRootFolderMap = new HashMap<String, String>();
		usernameRootFolderMap.put("gareyes", "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/Testing/2017_05_14/syncdata1"); // Gabriel
//		usernameRootFolderMap.put("gareyes", "/Volumes/HanSolo/Dropbox/Georgia Tech/Synchro/Data/User Study/Flat/sync1000"); // Gabriel
//		usernameRootFolderMap.put("jwpilly", "/Users/jwpilly/Downloads/2017_03_21/syncdata"); // Jason
        usernameRootFolderMap.put("jwpilly", "/Users/jwpilly/Downloads/2017_05_01/syncdata1");

		String username = System.getProperty("user.name");
		if (username != null && usernameRootFolderMap.containsKey(username))
		{
			inputFolder = usernameRootFolderMap.get(username);
		}
		else
		{
			logOut(TAG, "Please configure the folder paths for this new user and start again.");
			System.exit(0);
		}

		if(Config.USE_SYNTHETICDATA) {
			dataPath = inputFolder + "/synthdata";
		} else {
			dataPath = inputFolder + "/data";
		}
		outputPath = inputFolder + "/output";
		windowPath = inputFolder + "/windows";

		File dataFolder = new File(dataPath);
		File outputFolder = new File(outputPath);
		File windowFolder = new File(windowPath);
		dataFolder.mkdirs();
		outputFolder.mkdirs();
		windowFolder.mkdirs();

		Path dir = Paths.get(dataPath);
		try {
			DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
			for (Path file : stream) {
				if (!(file.getFileName().startsWith(".DS_Store"))) {
					if (file.getFileName().toString().contains("_raw.csv")) {
						logOut(TAG, "" + file.getFileName());
						calculateCorrelation(file);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	private static void calculateCorrelation(Path file) {

		SynchroDetector syncDetector = new SynchroDetector(false);
		syncDetector.setEventRecognitionListener(new EventRecognitionListener() {
			@Override
			public void onEventRecognized(String result) {
				if(debugMode) logOut(TAG, result);
			}
		});
		String inputFilePath = file.toString();
		String inputFileName = file.getFileName().toString();
		String outputBase = inputFileName.substring(0, inputFileName.length() - 7);
		String correlationOutputFile = outputPath + "/" + outputBase + "offline.csv";
		String featureOutputFile = outputPath + "/" + outputBase + "feature.csv";
		String windowsOutputFile = windowPath + "/" + outputBase + "window";
		String lagOutputFile = outputPath + "/" + outputBase + "lag.csv";
		String subsampleOutputFile = outputPath + "/" + outputBase + "raw.csv";
		String magnitudeOutputFile = outputPath + "/" + outputBase + "mag.csv";
		String deltaThreshOutputFile = outputPath + "/" + outputBase + "delta.csv";
		String windowTimesOutputFile = outputPath + "/" + outputBase + "times.csv";

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
				if (row[1].contains("begin")) {
					begun = true;
				}
				if (!begun) {
					continue;
				}
				if (row.length < 2) {
					continue;
				}
				if (row[1].equals("left")) {
					syncDetector.dataBuffer.offer(new Tuple2(null, new double[]{0, lastConvertedTS}));
				} else if (row[1].equals("right")) {
					syncDetector.dataBuffer.offer(new Tuple2(null, new double[]{1, lastConvertedTS}));
				}
				// old user study data
				if(row.length == 7 || (row.length == 8 && row[7].equals(""))) {
					if (row[1].equals("magnet")) {
						double sensorTimestamp = Double.parseDouble(row[5]);
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
				// new user study data
				else if (row.length == 8) {
					if (row[1].equals("magnet")) {
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

			writeMapToFile(syncDetector.getCorrelationMap(), correlationOutputFile);
			writeMapToFile(syncDetector.getFeatureMap(), featureOutputFile);
			writeMapToFile(syncDetector.getLagMap(), lagOutputFile);
			writeMapToFile(syncDetector.getSubsampleMap(), subsampleOutputFile);
			writeMapToFile(syncDetector.getMagnitudeMap(), magnitudeOutputFile);
//			writeWindowMapToFile(syncDetector.getWindowsMap(), windowsOutputFile);
			writeMapToFile(syncDetector.getSyncThresholdMap(), deltaThreshOutputFile);
			writeMapToFile(syncDetector.getWindowTimeMap(), windowTimesOutputFile);
			syncDetector.isRunning = false;
		} catch (Exception e) {
			System.out.println("error while processing file");
			e.printStackTrace();
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
				e.printStackTrace();
			}
		} else {
			logOut(TAG, "filename is null");
		}
	}

	private static void writeWindowMapToFile(Map<?,?> inputMap, String fileName) {
		if (!fileName.equals("")) {
			int windowCounter = 0;
			Object[] keys = inputMap.keySet().toArray();
			for (Object windowTime : keys) {
				String windowName = fileName + windowCounter + ".csv";
				try {
					File writeFile = new File(windowName);
					if (writeFile.exists()) {
						writeFile.delete();
					}
					PrintWriter writer = new PrintWriter(windowName, "UTF-8");
					String[] windowResults = (String[]) inputMap.get(windowTime);
					for (String row : windowResults) {
						writer.println(row);
					}
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				windowCounter++;
			}
		} else {
			logOut(TAG, "filename is null");
		}
	}
}
