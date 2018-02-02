package segmentation;

import model.Tuple;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Segments the data into the appropriate size
 * Created by Victor on 11/3/2016.
 */
public class Segmenter extends Observable implements Observer, Runnable{

    private String trainingLabel;

    private String segmentationType; //sliding window or events
    private int bufferSize;
    private int overlap;
    private BlockingDeque<Float> floatBuffer;
    private float[] data;
    private float[] overlapBuffer;

    private BlockingQueue<float[]> dataBuffer;

    private int count = 0;

    private boolean isDoneReceiving = false;

    public Segmenter(String segmentationType, int bufferSize, int overlap, String trainingLabel) {
        System.out.println("Segmenter was created");
        this.segmentationType = segmentationType;
        this.bufferSize = bufferSize;
        this.overlap = overlap;
        this.trainingLabel = trainingLabel;

        floatBuffer = new LinkedBlockingDeque<>();
        data = new float[bufferSize];
        overlapBuffer = new float[overlap];
        dataBuffer = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        boolean go = true;
        while (go) {
            if (!dataBuffer.isEmpty()) {
                try {
                    float[] temp = dataBuffer.take();

                    for (int i = 0; i < temp.length; i++) {
                        floatBuffer.offer(temp[i]);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (floatBuffer.size() >= bufferSize) {
                System.out.println(floatBuffer.size());
                for (int i = 0; i < data.length; i++) {
                    data[i] = floatBuffer.pollFirst();
                    if (segmentationType == "sliding") {
                        if (i >= (bufferSize - overlap)) {
                            overlapBuffer[i - (bufferSize - overlap)] = data[i];
                        }
                    }
                }
                //add overlap data back into deque
                System.out.println("readding overlap");
                for (int i = overlapBuffer.length - 1; i >= 0; i--) {
                    floatBuffer.offerFirst(overlapBuffer[i]);
                }
                System.out.println("after overlap added" + floatBuffer.size());


                setChanged();
                notifyObservers(data);
                System.out.println("############ " + ++count + " " + trainingLabel);
            }

            if (isDoneReceiving && dataBuffer.isEmpty() && floatBuffer.size() < bufferSize) {
                go = false;
                System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                setChanged();
                notifyObservers("done");
            }

        }
    }

    @Override
    public void update(Observable obj, Object arg) {
        if (arg instanceof Tuple) {
            Tuple data = (Tuple) arg;
            if (data.getTrainingLabel().equals(trainingLabel)) {
                addToBuffer(data.getInput().clone());
            }
//            System.out.println("segmenter just got a thing");
        } else if (arg instanceof String) {
            isDoneReceiving = true;
        } else {
//            System.out.println("Received object is invalid");
        }
    }


    private void addToBuffer(float[] floatVals) {
        dataBuffer.offer(floatVals);
    }

}
