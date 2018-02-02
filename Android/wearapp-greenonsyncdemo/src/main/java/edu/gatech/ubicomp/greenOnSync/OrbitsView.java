package edu.gatech.ubicomp.greenOnSync;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by gareyes on 8/18/16.
 */
public class OrbitsView extends View {

	private static Paint paint;
	private int screenW, screenH;
	private float X, Y;
	private Path path;
	private float initialScreenW;
	private float initialX, plusX;
	private float TX;
	private boolean translate;
	private int flash;
	private Context context;
	private TextView magnetTextView;
	//private Canvas canvas;
	private Integer magneticFieldValue = 0;
	private float sweepAngle = 0;
	public boolean drawPath = false;
	private boolean drawBoldPath = false;
	public int drawColor;
	private boolean drawLeft = false;
	private boolean drawRight = false;
	private boolean recordingFlag = false;
	private boolean inSyncFlag = false;
	//private String direction = "unknown";

	public OrbitsView(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.context = context;

		paint = new Paint();
		paint.setColor(Color.BLUE);
		//paint.setColor(Color.argb(0xff, 0x99, 0x00, 0x00));
		paint.setStrokeWidth(5);
		paint.setAntiAlias(true);
		//paint.setStrokeCap(Paint.Cap.ROUND);
		//paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStyle(Paint.Style.STROKE);
		//paint.setShadowLayer(, 0, 0, Color.RED);

		path = new Path();
		TX = 0;
		translate = false;

		flash = 0;

	}

	public void drawLeftCircle()
	{
		drawLeft = true;
		drawRight = false;
		//Log.v("drawLeftdrawLeft", drawLeft ? "true" : "false");
		//Log.v("drawRightdrawLeft", drawRight ? "true" : "false");
	}

	public void drawRightCircle()
	{
		drawLeft = false;
		drawRight = true;
		//Log.v("drawLeftdrawRight", drawLeft ? "true" : "false");
		//Log.v("drawRightdrawRight", drawRight ? "true" : "false");
	}

	public void setRecordingMode(boolean flag)
	{
		recordingFlag = flag;
	}
	public void setInSYnc(boolean flag) { inSyncFlag = flag;}
	//public void setSyncDirection(String direction) {this.direction = direction;}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		screenW = w;
		screenH = h;
		X = 0;
		Y = (screenH / 2) + (screenH / 4) + (screenH / 10);

		initialScreenW = screenW;
		initialX = ((screenW / 2) + (screenW / 4));
		plusX = (screenW / 24);

		path.moveTo(X, Y);
	}

	private final Paint mOvalPaint = new Paint() {
		{
			setStyle(Paint.Style.FILL);
			setColor(Color.RED);
		}
	};

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

//		paint.setColor(Color.WHITE);
//		paint.setTextSize(40);
//		canvas.drawText(Integer.toString(magneticFieldValue), screenW/2-40, screenH/2, paint);

		RectF mOvalLeft = new RectF(0, screenH/4, screenW/2, screenH*3/4); //This is the area you want to draw on
		RectF mOvalRight = new RectF(screenW/2, screenH/4, screenW, screenH*3/4); //This is the area you want to draw on

        //Log.v("drawLeftonDraw", drawLeft ? "true" : "false");
		//Log.v("drawRightonDraw", drawRight ? "true" : "false");
		if(recordingFlag && inSyncFlag) {

				mOvalPaint.setColor(Color.GREEN);



		}
		else if(recordingFlag)
		{
			mOvalPaint.setColor(Color.RED);
		}
		else {
			mOvalPaint.setColor(Color.BLUE);
		}

		if (drawLeft) canvas.drawOval(mOvalLeft, mOvalPaint);
		else if (drawRight) canvas.drawOval(mOvalRight, mOvalPaint);

		invalidate();
	}
}