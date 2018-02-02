package edu.gatech.ubicomp.synchro.livedatacollect;

/**
 * Created by gareyes on 2/7/17.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;


public class LineChartView extends View {
	private final static String TAG = LineChartView.class.getSimpleName();
	private final static boolean DEBUG = false;
	private int contentWidth;
	private int contentHeight;
	private float spacingBetweenPoints;
	private boolean firstTime = true;
	private String deviceName = "";
	private ArrayList<Float> xPoints, yPoints, zPoints;
	private Paint myPaint;

	private final static int X_AXIS_COLOR = Color.rgb(255, 0, 0);
	private final static int Y_AXIS_COLOR = Color.rgb(0, 255, 0);
	private final static int Z_AXIS_COLOR = Color.rgb(0, 155, 255);

	enum AXIS_TYPE {
		X_AXIS,
		Y_AXIS,
		Z_AXIS
	}

	public boolean isShowTrainingSample = true;

	public float maximumYRange;

	public LineChartView(Context context) {
		super(context);
		init();
	}

	public LineChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		myPaint = new Paint();

		xPoints = new ArrayList<Float>();
		yPoints = new ArrayList<Float>();
		zPoints = new ArrayList<Float>();
		maximumYRange = 1.0f;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// allocations per draw cycle.
		if (firstTime) {
			contentWidth = canvas.getWidth();
			contentHeight = canvas.getHeight();
			//Log.d(TAG, "Content height: " + contentHeight+ " ContentWidth: "+ contentWidth);
			firstTime = false;
		}

//        if(deviceName.equals("phone"))
//        {
//            int winLen = 400;//(int) (Constants.SAMPLING_RATE_PHONE*(Config.GESTURE_LENGTH_IN_TIME/Constants.MS_IN_SEC));
//            spacingBetweenPoints = (1.0f * getWidth()) / winLen; //multiplied by 1.0f to preserve the fraction value
//            setGraphHeightRange(Constants.PHONE_GYRO_VALUE_RANGE);
//        } else if (deviceName.equals("watch"))
//        {
//            int winLen = 32;//(int) (Constants.SAMPLING_RATE_WEAR*(Config.GESTURE_LENGTH_IN_TIME/Constants.MS_IN_SEC));
//            spacingBetweenPoints = (1.0f * getWidth()) / winLen; //multiplied by 1.0f to preserve the fraction value
//            setGraphHeightRange(Constants.MOTO360_GYRO_VALUE_RANGE);
//        }

		if (isShowTrainingSample && xPoints.size() > 1) {
			myPaint.setColor(Color.argb(128, 128, 128, 128));
			myPaint.setStrokeWidth(1);
			//canvas.drawLine(0, contentHeight * 0.5f * 0.33f, getWidth(), contentHeight * 0.5f * 0.33f, myPaint);
			canvas.drawLine(0, contentHeight * 0.5f, getWidth(), contentHeight * 0.5f, myPaint);
			//canvas.drawLine(0, contentHeight * (0.66f + 0.33f * 0.5f), getWidth(), contentHeight * (0.66f + 0.33f * 0.5f), myPaint);

			myPaint.setStrokeWidth(2);
			myPaint.setColor(X_AXIS_COLOR);

			//canvas.drawLines(ArrayUtils.toPrimitive(xPoints.toArray(new Float[0]), 0.0F), myPaint);

			for(int i=0; i<xPoints.size(); i++) {
				canvas.drawPoint(i, xPoints.get(i), myPaint);
			}

//			myPaint.setColor(Y_AXIS_COLOR);
//			canvas.drawLines(ArrayUtils.toPrimitive(yPoints.toArray(new Float[0]), 0.0F), myPaint);
//
//			myPaint.setColor(Z_AXIS_COLOR);
//			canvas.drawLines(ArrayUtils.toPrimitive(zPoints.toArray(new Float[0]), 0.0F), myPaint);
		}
	}

	public void addPoint(float p) {
		float newValue = contentHeight * 0.5f * (1 - p / maximumYRange);
		while(xPoints.size() > contentWidth) xPoints.remove(0);
		xPoints.add(newValue);
		//Log.d("tag", xPoints.toString());
		//xPoints.add(convertToCartesian(p, AXIS_TYPE.X_AXIS));
		this.postInvalidate();
	}

	public void addPoint(float[] p) {
		float newXLocation = xPoints.size() * spacingBetweenPoints * 0.25f;

		xPoints.add(newXLocation);
		yPoints.add(newXLocation);
		zPoints.add(newXLocation);

		xPoints.add(convertToCartesian(p[0], AXIS_TYPE.X_AXIS));
		yPoints.add(convertToCartesian(p[1], AXIS_TYPE.Y_AXIS));
		zPoints.add(convertToCartesian(p[2], AXIS_TYPE.Z_AXIS));

		if (xPoints.size() > 2) {
			xPoints.add(newXLocation);
			yPoints.add(newXLocation);
			zPoints.add(newXLocation);

			xPoints.add(convertToCartesian(p[0], AXIS_TYPE.X_AXIS));
			yPoints.add(convertToCartesian(p[1], AXIS_TYPE.Y_AXIS));
			zPoints.add(convertToCartesian(p[2], AXIS_TYPE.Z_AXIS));
		}

		this.postInvalidate();
	}

	public void clearViz() {
		xPoints.clear();
		yPoints.clear();
		zPoints.clear();
		this.postInvalidate();
	}

	public void showTrainingSample(ArrayList<Float[]> points) {
		isShowTrainingSample = true;
		clearViz();
		Float[] xValues = points.get(0);
		Float[] yValues = points.get(1);
		Float[] zValues = points.get(2);
		spacingBetweenPoints = (1.0f * getWidth()) / xValues.length;
		for (int i = 0 ; i < xValues.length; i++) {
			float newXLocation = xPoints.size() * spacingBetweenPoints * 0.25f;

			xPoints.add(newXLocation);
			yPoints.add(newXLocation);
			zPoints.add(newXLocation);

			xPoints.add(convertToCartesian(xValues[i], AXIS_TYPE.X_AXIS));
			yPoints.add(convertToCartesian(yValues[i], AXIS_TYPE.Y_AXIS));
			zPoints.add(convertToCartesian(zValues[i], AXIS_TYPE.Z_AXIS));

			if (xPoints.size() > 2) {
				xPoints.add(newXLocation);
				yPoints.add(newXLocation);
				zPoints.add(newXLocation);

				xPoints.add(convertToCartesian(xValues[i], AXIS_TYPE.X_AXIS));
				yPoints.add(convertToCartesian(yValues[i], AXIS_TYPE.Y_AXIS));
				zPoints.add(convertToCartesian(zValues[i], AXIS_TYPE.Z_AXIS));
			}
		}
		this.postInvalidate();
	}

	/**
	 * Set the maximum allowable vertical range for the plot based on range of sensor values.
	 */
	public void setGraphHeightRange(float value) {
		maximumYRange = value;
	}

	private Float convertToCartesian(float p, AXIS_TYPE axisType) {
		float value = contentHeight * 0.5f * (1 - p / maximumYRange);
//        switch (axisType) {
//            case X_AXIS:
//                return value;
//            case Y_AXIS:
//                return contentHeight * 0.33f + value;
//            case Z_AXIS:
//                return contentHeight * 0.66f + value;
//        }
//        return null;
		return value;
	}

	private float[] convertToPrimitive() {
		float[] arr = new float[xPoints.size()];
		int i = 0;
		for (Float f : xPoints) {
			arr[i] = f;
		}
		return arr;
	}

	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}
}