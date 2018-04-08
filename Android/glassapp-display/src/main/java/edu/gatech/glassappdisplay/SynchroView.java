package edu.gatech.glassappdisplay;


import android.content.Context;
import android.graphics.*;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by gareyes on 8/18/16.
 */
public class SynchroView extends View {

    private String TAG = this.getClass().getSimpleName();

    private boolean debugMode = false;

    private Paint leftCirclePaint, rightCirclePaint, boundPaint, indPaint;
    public RectF leftCircle, rightCircle, boundRect;
    public int screenW, screenH;
    private boolean drawLeft = false;
    private boolean drawRight = false;
    private float left, top, right, bottom;
    private double gyroDraw = 0;
    private long lastCross = 0;

    private boolean showNotif = false;

    private boolean peripheral = true;

    public SynchroView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public void setGyroDraw(double value) {
        Log.d("setGyroDraw", "" + value);
        gyroDraw = value;
    }

    public void setShowNotif(boolean notif) {
        showNotif = notif;
    }

    public boolean getShowNotif() {
        return showNotif;
    }

    private void initView() {
        if(debugMode) Log.d(TAG, "initview");

        // instantiate objects
        leftCircle = new RectF();
        rightCircle = new RectF();
        boundRect = new RectF();

        // paint for left target
        leftCirclePaint = new Paint();
        leftCirclePaint.setStyle(Paint.Style.FILL);
        leftCirclePaint.setColor(Color.BLUE);

        // paint for right target
        rightCirclePaint = new Paint();
        rightCirclePaint.setStyle(Paint.Style.FILL);
        rightCirclePaint.setColor(Color.BLUE);

        boundPaint = new Paint();
        boundPaint.setStyle(Paint.Style.FILL);
        boundPaint.setColor(Color.WHITE);

        indPaint = new Paint();
        indPaint.setStyle(Paint.Style.FILL);
        indPaint.setColor(Color.GREEN);

        // set defaults
        drawLeft = false;
        drawRight = false;

        // set circles
        setCircleLocations();

        invalidate();
    }

    public void setCircleLocations() {
        if(debugMode) Log.d(TAG, "setcirclelocations");
        if(debugMode) Log.d(TAG, "inside set for non debug");

        top = 0;
        left = (screenW * 11 / 12) - (Config.ICON_WIDTH / 2);
        right = (screenW * 11 / 12) + (Config.ICON_WIDTH / 2);
        bottom = Config.ICON_HEIGHT;
        if(debugMode) Log.d(TAG, "" + left + "," + top + "," + right + "," + bottom);
        leftCircle.set(left, top, right, bottom);

        top = screenH - Config.ICON_HEIGHT;
        left = (screenW * 11 / 12) - (Config.ICON_WIDTH / 2);
        right = (screenW * 11 / 12) + (Config.ICON_WIDTH / 2);
        bottom = screenH;
        if(debugMode) Log.d(TAG, "" + left + "," + top + "," + right + "," + bottom);
        rightCircle.set(left, top, right, bottom);
        invalidate();
    }

    public void drawLeftCircle() {
        if(debugMode) Log.d(TAG, "drawleftcircle");
        drawLeft = true;
        drawRight = false;
        invalidate();
    }

    public void drawRightCircle() {
        if(debugMode) Log.d(TAG, "drawrightcircle");
        drawLeft = false;
        drawRight = true;
        invalidate();
    }

    public void clearCircles() {
        if(debugMode) Log.d(TAG, "clearcircles");
        drawLeft = false;
        drawRight = false;
        invalidate();
    }

    public void setFeedback(float score, String direction) {
//		double newScaleFactor = Math.max((score - 0.35),0) / (1 - 0.35);
        double newScaleFactor = score;
        if(direction.equals("top")) {
            leftCirclePaint.setColor(Color.rgb(0,(int)(newScaleFactor*255), 255 - (int)(newScaleFactor*255)));
            rightCirclePaint.setColor(Color.rgb(0, 0, 255));
        } else if (direction.equals("bottom")) {
            leftCirclePaint.setColor(Color.rgb(0, 0, 255));
            rightCirclePaint.setColor(Color.rgb(0,(int)(newScaleFactor*255), 255 - (int)(newScaleFactor*255)));
        }
        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(debugMode) Log.d(TAG, "onsizechanged");
        screenW = w;
        screenH = h;
        setCircleLocations();
    }

    public long getLastCross() {
        return lastCross;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(debugMode) Log.d(TAG + " drawing", "" + drawLeft + "," + drawRight);

        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);

        if (showNotif) {
            if (peripheral) {
            if (drawLeft) {
                paint.setColor(Color.RED);
                canvas.drawPaint(paint);
            }
            if (drawRight) {
                paint.setColor(Color.GREEN);
                canvas.drawPaint(paint);
            }
        }
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(40);
            int xPos = (canvas.getWidth() / 2);
            int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));
            canvas.drawText("Notification!", xPos, yPos, paint);

            Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.bell);
            canvas.drawBitmap(b, b.getWidth() / 2, canvas.getHeight() / 2 - (b.getHeight() / 2), paint);

            if (drawLeft) canvas.drawOval(leftCircle, leftCirclePaint);
            if (drawRight) canvas.drawOval(rightCircle, rightCirclePaint);
        }
    }
}
