package edu.gatech.ubicomp.synchro.detector;

/**
 * Created by jwpilly on 9/8/16.
 */
public class Tuple2<T> {
    private T x;
    private T y;

    public Tuple2(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public T getX() {
        return x;
    }

    public T getY() {
        return y;
    }

    public Tuple2<T> reversed() {
        return new Tuple2<>(y, x);
    }
}
