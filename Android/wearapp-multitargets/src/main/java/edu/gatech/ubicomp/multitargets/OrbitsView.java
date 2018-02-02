package edu.gatech.ubicomp.multitargets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by gareyes on 8/18/16.
 */
public class OrbitsView extends View {

	private static Paint paint;
	private int screenW, screenH;
	private float X, Y;
	private Path path;
	//private float initialScreenW;
	//private float initialX, plusX;
	//private float TX;

	//private boolean translate;
	//private int flash;
	//private Context context;
	//private TextView magnetTextView;
	//private Canvas canvas;
	//private Integer magneticFieldValue = 0;
	//private float sweepAngle = 0;
	//public boolean drawPath = false;
	//private boolean drawBoldPath = false;
	//public int drawColor;
	private boolean drawLeft = false;
	private boolean drawRight = false;
	private boolean drawTop = false;
	private boolean drawBottom = false;
	private boolean recordingMode = false;
	private boolean inSyncFlag = false;
	private boolean directionVertical = false;

	private String syncDirection = "unknown";

	public OrbitsView(Context context, AttributeSet attrs) {
		super(context, attrs);

		//this.context = context;

		paint = new Paint();
		paint.setColor(Color.BLUE);
		//paint.setColor(Color.argb(0xff, 0x99, 0x00, 0x00));
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);
		//paint.setStrokeCap(Paint.Cap.ROUND);
		//paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStyle(Paint.Style.STROKE);
		//paint.setShadowLayer(, 0, 0, Color.RED);

		path = new Path();
		//TX = 0;
		//translate = false;

		//flash = 0;

	}

	public void drawLeftCircle()
	{
		if(directionVertical)
		{
			drawLeft = false;
			drawRight = false;
			drawTop = true;
			drawBottom = false;
		}
		else
		{
			drawLeft = true;
			drawRight = false;
			drawTop = false;
			drawBottom = false;
		}

	}

	public void drawRightCircle()
	{
		if(directionVertical)
		{
			drawLeft = false;
			drawRight = false;
			drawTop = false;
			drawBottom = true;
		}
		else
		{
			drawLeft = false;
			drawRight = true;
			drawTop = false;
			drawBottom = false;
		}

	}

	public void setRecordingMode(boolean flag)
	{
		recordingMode = flag;
	}
	public void setDirection(boolean vertical)
	{
		directionVertical = vertical;
	}
	public void setInSYnc(boolean flag) { inSyncFlag = flag;}
	public void setSyncDirection(String direction)
	{
		syncDirection = direction;
	}
	public String getSyncDirection()
	{
		return syncDirection;
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		screenW = w;
		screenH = h;
		X = 0;
		Y = (screenH / 2) + (screenH / 4) + (screenH / 10);

		//initialScreenW = screenW;
		//initialX = ((screenW / 2) + (screenW / 4));
		//plusX = (screenW / 24);

		path.moveTo(X, Y);
	}

	private final Paint mOvalPaint = new Paint() {
		{
			setStyle(Style.FILL);
			setColor(Color.BLUE);
		}
	};

	private final Paint mOvalPaint2 = new Paint() {
		{
			setStyle(Style.FILL);
			setColor(Color.rgb(77, 163, 255));
		}
	};

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

//		paint.setColor(Color.WHITE);
//		paint.setTextSize(40);
//		canvas.drawText(Integer.toString(magneticFieldValue), screenW/2-40, screenH/2, paint);

		RectF mOvalLeft = new RectF(11, screenH/2 -10, 31, screenH/2 + 10); //This is the area you want to draw on
		RectF mOvalRight = new RectF(screenW-31, screenH/2 -10, screenW-11, screenH/2 +10); //This is the area you want to draw on
		RectF mOvalTop = new RectF(screenW/2 -10, 11, screenW/2 +10, 31); //This is the area you want to draw on
		RectF mOvalBottom = new RectF(screenW/2 -10, screenH - 31, screenW/2 +10, screenH-11); //This is the area you want to draw on

        //Log.v("drawLeftonDraw", drawLeft ? "true" : "false");
		//Log.v("drawRightonDraw", drawRight ? "true" : "false");
		if(recordingMode && inSyncFlag) {
			mOvalPaint.setColor(Color.rgb(7,104,7));
		}
		else if(recordingMode)
		{
			mOvalPaint.setColor(Color.rgb(77, 163, 255));
		}
		else {
			mOvalPaint.setColor(Color.rgb(77, 163, 255));
		}

		if(syncDirection.equals("left"))
		{
			if (drawLeft) canvas.drawOval(mOvalLeft, mOvalPaint);
			else if (drawRight) canvas.drawOval(mOvalRight, mOvalPaint2);
			else if (drawTop) canvas.drawOval(mOvalTop, mOvalPaint);
			else if (drawBottom) canvas.drawOval(mOvalBottom, mOvalPaint2);
		}
		else if(syncDirection.equals("right"))
		{
			if (drawLeft) canvas.drawOval(mOvalLeft, mOvalPaint2);
			else if (drawRight) canvas.drawOval(mOvalRight, mOvalPaint);
			else if (drawTop) canvas.drawOval(mOvalTop, mOvalPaint2);
			else if (drawBottom) canvas.drawOval(mOvalBottom, mOvalPaint);
		}
		else {
			if (drawLeft) canvas.drawOval(mOvalLeft, mOvalPaint);
			else if (drawRight) canvas.drawOval(mOvalRight, mOvalPaint);
			else if (drawTop) canvas.drawOval(mOvalTop, mOvalPaint);
			else if (drawBottom) canvas.drawOval(mOvalBottom, mOvalPaint);
		}
		invalidate();
	}
}