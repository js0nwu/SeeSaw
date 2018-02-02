package edu.gatech.ubicomp.synchro.swipedatacollect;

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
public class SwipeView extends View {

	private String TAG = this.getClass().getSimpleName();

	private boolean debugMode = false;

	private Paint leftCirclePaint, rightCirclePaint, targetAreaPaint;
	public RectF leftCircle, rightCircle, leftTargetArea, rightTargetArea;
	public float screenW, screenH;
	private float left, top, right, bottom;

	public SwipeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	private void initView() {
		// paint for touch/release targets
		targetAreaPaint = new Paint();
		targetAreaPaint.setStyle(Paint.Style.STROKE);
		targetAreaPaint.setStrokeWidth(3);
		targetAreaPaint.setColor(Color.CYAN);

		// paint for left target
		leftCirclePaint = new Paint();
		leftCirclePaint.setStyle(Paint.Style.FILL);
		leftCirclePaint.setColor(Color.BLUE);

		// paint for right target
		rightCirclePaint = new Paint();
		rightCirclePaint.setStyle(Paint.Style.FILL);
		rightCirclePaint.setColor(Color.BLUE);

		invalidate();
	}

	public void setSwipeDetectedColors() {
		leftCirclePaint.setColor(Color.GREEN);
		rightCirclePaint.setColor(Color.GREEN);
		invalidate();
	}

	public void resetSwipeDetectedColors() {
		leftCirclePaint.setColor(Color.BLUE);
		rightCirclePaint.setColor(Color.BLUE);
		invalidate();
	}

	public void resetCircleLocations() {

		if(debugMode) Log.d(TAG, "screenW " + screenW);
		if(debugMode) Log.d(TAG, "screenH " + screenH);
		if(debugMode) Log.d(TAG, "X_FACTOR " + Config.X_FACTOR);
		if(debugMode) Log.d(TAG, "Y_FACTOR " + Config.Y_FACTOR);

		// left side

		left = 0;
		top = (screenH/2) - (Config.ICON_HEIGHT /2);
		right = Config.ICON_WIDTH;
		bottom = (screenH/2) + (Config.ICON_HEIGHT /2);
		leftCircle = new RectF(left, top, right, bottom);

		left = 0 - (Config.ICON_WIDTH * Config.X_FACTOR);
		top = (screenH/2)  - (Config.ICON_HEIGHT * Config.Y_FACTOR);
		right = 0 + (Config.ICON_WIDTH * Config.X_FACTOR);
		bottom = (screenH/2) + (Config.ICON_HEIGHT * Config.Y_FACTOR);
		if(debugMode) Log.d(TAG, "leftTargetArea" + "," + left + "," + top + "," + right + "," + bottom);
		leftTargetArea = new RectF(left, top, right, bottom);

		// right side

		left = screenW - Config.ICON_WIDTH;
		top = (screenH/2) - (Config.ICON_HEIGHT /2);
		right = screenW;
		bottom = (screenH/2) + (Config.ICON_HEIGHT /2);
		rightCircle = new RectF(left, top, right, bottom);

		left = screenW - (Config.ICON_WIDTH * Config.X_FACTOR);
		top = (screenH/2)  - (Config.ICON_HEIGHT * Config.Y_FACTOR);
		right = screenW + (Config.ICON_WIDTH * Config.X_FACTOR);
		bottom = (screenH/2) + (Config.ICON_HEIGHT * Config.Y_FACTOR);
		if(debugMode) Log.d(TAG, "rightTargetArea" + "," + left + "," + top + "," + right + "," + bottom);
		rightTargetArea = new RectF(left, top, right, bottom);

		invalidate();
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		screenW = w;
		screenH = h;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawOval(leftCircle, leftCirclePaint);
		canvas.drawOval(rightCircle, rightCirclePaint);
		if(Config.SHOW_TOUCH_ZONE) canvas.drawRect(leftTargetArea, targetAreaPaint);
		if(Config.SHOW_TOUCH_ZONE) canvas.drawRect(rightTargetArea, targetAreaPaint);
	}
}