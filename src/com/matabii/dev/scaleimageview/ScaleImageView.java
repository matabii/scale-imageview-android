package com.matabii.dev.scaleimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class ScaleImageView extends ImageView implements OnTouchListener {
	private float MAX_SCALE = 2f;
	private int DOUBLE_TAP_SECOND = 400;

	private Matrix mMatrix;
	private final float[] mMatrixValues = new float[9];

	private int mIntrinsicWidth;
	private int mIntrinsicHeight;

	private float mScale;
	private float mMinScale;

	// double tap for determining
	private long mLastTime = 0;
	private boolean isDoubleTap;
	private int mDoubleTapX;
	private int mDoubleTapY;

	private float mPrevDistance;
	private boolean isScaling;

	private int mPrevMoveX;
	private int mPrevMoveY;

	String TAG = "ScaleImageView";

	public ScaleImageView(Context context, AttributeSet attr) {
		super(context, attr);
		initialize();
	}

	public ScaleImageView(Context context) {
		super(context);
		initialize();
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		this.initialize();
	}

	private void initialize() {
		this.setScaleType(ScaleType.MATRIX);
		this.mMatrix = new Matrix();
		Drawable d = getDrawable();
		if (d != null) {
			mIntrinsicWidth = d.getIntrinsicWidth();
			mIntrinsicHeight = d.getIntrinsicHeight();
			setOnTouchListener(this);
		}
	}

	@Override
	protected boolean setFrame(int l, int t, int r, int b) {
		mMatrix.reset();
		mScale = (float) r / (float) mIntrinsicWidth;
		int paddingHeight = 0;
		int paddingWidth = 0;
		// scaling vertical
		if (mScale * mIntrinsicHeight > b) {
			mScale = (float) b / (float) mIntrinsicHeight;
			int width = (int) ((float) mScale * (float) mIntrinsicWidth);
			mMatrix.postScale(mScale, mScale);
			paddingWidth = (r - width) / 2;
			paddingHeight = 0;
			// scaling horizontal
		} else {
			mMatrix.postScale(mScale, mScale);
			int height = (int) ((float) mScale * (float) mIntrinsicHeight);
			paddingHeight = (b - height) / 2;
			paddingWidth = 0;
		}
		mMatrix.postTranslate(paddingWidth, paddingHeight);

		setImageMatrix(mMatrix);
		mMinScale = mScale;
		return super.setFrame(l, t, r, b);
	}

	protected float getValue(Matrix matrix, int whichValue) {
		matrix.getValues(mMatrixValues);
		return mMatrixValues[whichValue];
	}

	protected float getScale() {
		return getValue(mMatrix, Matrix.MSCALE_X);
	}

	protected float getTranslateX() {
		return getValue(mMatrix, Matrix.MTRANS_X);
	}

	protected float getTranslateY() {
		return getValue(mMatrix, Matrix.MTRANS_Y);
	}

	protected void maxZoomTo(int x, int y) {
		if (mMinScale != getScale() && (getScale() - mMinScale) > 0.1f) {
			// threshold 0.1f
			float scale = mMinScale / getScale();
			zoomTo(scale, x, y);
		} else {
			float scale = MAX_SCALE / getScale();
			zoomTo(scale, x, y);
		}
	}

	protected void zoomTo(float scale, int x, int y) {
		if (getScale() * scale < mMinScale) {
			return;
		}
		if (scale >= 1 && getScale() * scale > MAX_SCALE) {
			return;
		}
		mMatrix.postScale(scale, scale);
		// move to center
		mMatrix.postTranslate(-(getWidth() * scale - getWidth()) / 2,
				-(getHeight() * scale - getHeight()) / 2);

		// move x and y distance
		mMatrix.postTranslate(-(x - (getWidth() / 2)) * scale, 0);
		mMatrix.postTranslate(0, -(y - (getHeight() / 2)) * scale);
		setImageMatrix(mMatrix);
	}

	public void cutting() {
		int width = (int) (mIntrinsicWidth * getScale());
		int height = (int) (mIntrinsicHeight * getScale());
		if (getTranslateX() < -(width - getWidth())) {
			mMatrix.postTranslate(-(getTranslateX() + width - getWidth()), 0);
		}
		if (getTranslateX() > 0) {
			mMatrix.postTranslate(-getTranslateX(), 0);
		}
		if (getTranslateY() < -(height - getHeight())) {
			mMatrix.postTranslate(0, -(getTranslateY() + height - getHeight()));
		}
		if (getTranslateY() > 0) {
			mMatrix.postTranslate(0, -getTranslateY());
		}
		if (width < getWidth()) {
			mMatrix.postTranslate((getWidth() - width) / 2, 0);
		}
		if (height < getHeight()) {
			mMatrix.postTranslate(0, (getHeight() - height) / 2);
		}
		setImageMatrix(mMatrix);
	}

	private float distance(float x0, float x1, float y0, float y1) {
		float x = x0 - x1;
		float y = y0 - y1;
		return FloatMath.sqrt(x * x + y * y);
	}

	private float dispDistance() {
		return FloatMath.sqrt(getWidth() * getWidth() + getHeight()
				* getHeight());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int touchCount = event.getPointerCount();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_1_DOWN:
		case MotionEvent.ACTION_POINTER_2_DOWN:
			if (touchCount >= 2) {
				float distance = distance(event.getX(0), event.getX(1),
						event.getY(0), event.getY(1));
				mPrevDistance = distance;
				isScaling = true;
			} else {
				if (System.currentTimeMillis() <= mLastTime + DOUBLE_TAP_SECOND) {
					if (30 > Math.abs(mPrevMoveX - event.getX())
							+ Math.abs(mPrevMoveY - event.getY())) {
						isDoubleTap = true;
						mDoubleTapX = (int) event.getX();
						mDoubleTapY = (int) event.getY();
					}
				}
				mLastTime = System.currentTimeMillis();
				mPrevMoveX = (int) event.getX();
				mPrevMoveY = (int) event.getY();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (touchCount >= 2 && isScaling) {
				float dist = distance(event.getX(0), event.getX(1),
						event.getY(0), event.getY(1));
				float scale = (dist - mPrevDistance) / dispDistance();
				mPrevDistance = dist;
				scale += 1;
				scale = scale * scale;
				zoomTo(scale, getWidth() / 2, getHeight() / 2);
				cutting();
			} else if (!isScaling) {
				int distanceX = mPrevMoveX - (int) event.getX();
				int distanceY = mPrevMoveY - (int) event.getY();
				mPrevMoveX = (int) event.getX();
				mPrevMoveY = (int) event.getY();
				mMatrix.postTranslate(-distanceX, -distanceY);
				cutting();
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_POINTER_2_UP:
			if (event.getPointerCount() <= 1) {
				isScaling = false;
				if (isDoubleTap) {
					if (30 > Math.abs(mDoubleTapX - event.getX())
							+ Math.abs(mDoubleTapY - event.getY())) {
						maxZoomTo(mDoubleTapX, mDoubleTapY);
						cutting();
					}
				}
			}
			isDoubleTap = false;
			break;
		}
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return super.onTouchEvent(event);
	}
}