import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import model.Command;
import pipeline.Pipeline;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Example program to demonstrate how to use the Pipeline
 * In this case, program reads in multiple WAV files each with a different class name and calculates features for each
 *
 * Created by Victor on 11/7/2016.
 */
public class PipelineTester {

    public static void main(String[] args) {
        String filePath = "./MLearning/testdata";
        String shortFilePath = "./MLearning/shortdata";
        String arffFilePath = "./MLearning/arfffiles";

        String[] classLabels = {"ridgesCW", "ridgesCCW", "noise", "pyramids", "corner"};
        String gestureName;
        File folder = new File(filePath);
        File shortFolder = new File(shortFilePath);

        int bufferSize = 30000;
        int overlap = 15000;

        //Instantiate Pipeline object for calculating MFCC's with a sliding window, etc.
        final Pipeline pip = new Pipeline("sliding", bufferSize, overlap, "MFCC", classLabels, arffFilePath,
                new Command() {
                    public void execute(String s) {
                        System.out.println(s);
                    }
                });
        //Configure settings for MFCC
        pip.setMFCC(bufferSize, 48000, 26, 26, 0.0f, 48000/2f);


//      Trim 5 seconds off beginning and end of wav file
        for (File fileEntry : folder.listFiles()) {
            System.out.println(fileEntry.getName());
            String[] parts = fileEntry.getName().split("\\.");
            gestureName = parts[0];
            System.out.println(gestureName);
            String copyPath = shortFilePath + "/" + gestureName + "-short.wav";

            copyAudio(fileEntry.getAbsolutePath(), copyPath, 5, 60); //trim 5 seconds off each end 70 second file
        }

        //Go through each trimmed file and calculate features for each file/class
        for (File fileEntry: shortFolder.listFiles()) {
            AudioDispatcher dispatcher = null;
            final String[] parts = fileEntry.getName().split("-");
            System.out.println(fileEntry.getName());

            //Each file gets its own Segmenter and FeatureExtractor
            //Takes in a label so that the Pipeline can differentiate the data
            pip.createBranch(parts[0]);

            try {
                dispatcher = AudioDispatcherFactory.fromFile(fileEntry, bufferSize, 0);
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
            if (dispatcher != null) {
                dispatcher.addAudioProcessor(new AudioProcessor() {
                    @Override
                    public void processingFinished() {
//                        pip.finish();
//                        System.out.println("finished");
                    }

                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        //Pass in the float array into the pipeline
                        //Label needs to be the same as the label given to the branch
                        pip.read(audioEvent.getFloatBuffer(), parts[0]);
                        System.out.println("processing event" + parts[0]);

                        return true;
                    }
                });

                dispatcher.run();
            } else {
                System.out.println("DISPATCHER FROM FILE FAILED");
            }
        }
        //No more data needs to be passed into the Pipeline, so signal it to "finish" and tell
        //segmenters and featureExtractors that no more data will be passed in
        pip.finish();
    }

    /**
     * Trims audio data
     * @param sourceFileName file path
     * @param destinationFileName destination file path
     * @param startSecond get rid of the second before this number
     * @param secondsToCopy number of seconds to copy after the startSecond
     */
    public static void copyAudio(String sourceFileName, String destinationFileName, int startSecond, int secondsToCopy) {
        AudioInputStream inputStream = null;
        AudioInputStream shortenedStream = null;
        try {
            File file = new File(sourceFileName);
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            AudioFormat format = fileFormat.getFormat();
            inputStream = AudioSystem.getAudioInputStream(file);
            int bytesPerSecond = format.getFrameSize() * (int)format.getFrameRate();
            inputStream.skip(startSecond * bytesPerSecond);
            long framesOfAudioToCopy = secondsToCopy * (int)format.getFrameRate();
            shortenedStream = new AudioInputStream(inputStream, format, framesOfAudioToCopy);
            File destinationFile = new File(destinationFileName);
            AudioSystem.write(shortenedStream, fileFormat.getType(), destinationFile);
            System.out.println(fileFormat.getType());
            System.out.println(format.getFrameRate());
            System.out.println(format.getFrameSize());
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (inputStream != null) try { inputStream.close(); } catch (Exception e) { System.out.println(e); }
            if (shortenedStream != null) try { shortenedStream.close(); } catch (Exception e) { System.out.println(e); }
        }
    }

}
