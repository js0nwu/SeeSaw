package classification;

/**
 * Created by jwpilly on 9/8/16.
 */
public abstract class Detector<T> {
    protected EventRecognitionListener listener;

    public abstract boolean detect(T data);
    public abstract String detectDirection(T data);
    public void setEventRecognitionListener(EventRecognitionListener listener) {
        this.listener = listener;
    }
}
