package edu.gatech.ubicomp.synchro.livedatacollect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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

	public SynchroView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public void setGyroDraw(double value) {
		Log.d("setGyroDraw", "" + value);
		gyroDraw = value;
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

		if(Config.DEBUG_VIZ) {
			if(debugMode) Log.d(TAG, "inside set for debug");

			left = 0;
			top = (screenH / 2) - (Config.ICON_HEIGHT / 2);
			right = Config.ICON_WIDTH;
			bottom = (screenH / 2) + (Config.ICON_HEIGHT / 2);
			leftCircle.set(left, top, right, bottom);

			left = screenW - 30;
			top = 0;
			right = screenW;
			bottom = 30;
			rightCircle.set(left, top, right, bottom);

		} else {
			if(debugMode) Log.d(TAG, "inside set for non debug");

			left = 0;
			top = (screenH / 2) - (Config.ICON_HEIGHT / 2);
			right = Config.ICON_WIDTH;
			bottom = (screenH / 2) + (Config.ICON_HEIGHT / 2);
			if(debugMode) Log.d(TAG, "" + left + "," + top + "," + right + "," + bottom);
			leftCircle.set(left, top, right, bottom);

			left = screenW - Config.ICON_WIDTH;
			top = (screenH / 2) - (Config.ICON_HEIGHT / 2);
			right = screenW;
			bottom = (screenH / 2) + (Config.ICON_HEIGHT / 2);
			if(debugMode) Log.d(TAG, "" + left + "," + top + "," + right + "," + bottom);
			rightCircle.set(left, top, right, bottom);
		}
		if (Config.SUBTLE_FACTOR != 0) {
			float bleft = (screenW / 2) - (Config.SUBTLE_BOUND_LENGTH / 2);
			float bright = (screenW / 2) + (Config.SUBTLE_BOUND_LENGTH / 2);
			float btop = (screenH / 2) - (Config.SUBTLE_BOUND_HEIGHT / 2);
			float bbottom = (screenH / 2) + (Config.SUBTLE_BOUND_HEIGHT / 2);
			boundRect.set(bleft, btop, bright, bbottom);
		}
		invalidate();
	}

	public void drawLeftCircle() {
		if(debugMode) Log.d(TAG, "drawleftcircle");
		if (!Config.AWAKE_MODE) {
			drawLeft = true;
		} else {
			drawLeft = false;
		}
		drawRight = false;
		invalidate();
	}

	public void drawRightCircle() {
		if(debugMode) Log.d(TAG, "drawrightcircle");
		drawLeft = false;
		if (!Config.AWAKE_MODE) {
			drawRight = true;
		} else {
			drawRight = false;
		}
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
		if(direction.equals("left")) {
			leftCirclePaint.setColor(Color.rgb(0,(int)(newScaleFactor*255), 255 - (int)(newScaleFactor*255)));
			rightCirclePaint.setColor(Color.rgb(0, 0, 255));
		} else if (direction.equals("right")) {
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

		if(Config.DEBUG_VIZ) {
			if(drawRight) canvas.drawOval(rightCircle, rightCirclePaint);
		} else {
			if (drawLeft) canvas.drawOval(leftCircle, leftCirclePaint);
			if (drawRight) canvas.drawOval(rightCircle, rightCirclePaint);
		}

		if (Config.SUBTLE_FACTOR != 0) {
		    canvas.drawRect(boundRect, boundPaint);
			double pointY = (screenH / 2) + (Config.SUBTLE_FACTOR * gyroDraw * 10);
			if (pointY <= boundRect.bottom && pointY >= boundRect.top) {
				indPaint.setColor(Color.GREEN);
			} else {
				indPaint.setColor(Color.RED);
				lastCross = System.currentTimeMillis();
			}
			canvas.drawCircle(screenW / 2, (int) pointY, 10, indPaint);
		}
	}
}