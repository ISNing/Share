package org.exthmui.share.ui;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import org.exthmui.share.R;
import org.exthmui.share.shared.PorterDuffUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WaveView extends View {
    public static final int WAVING_MODE_LIMITED = 0x01;
    public static final int WAVING_MODE_NON_LIMITED = 0x02;
    private static final float DEFAULT_LENGTH_DIP = 200;
    private final float DEFAULT_LENGTH_PX = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 200f, getResources().getDisplayMetrics());

    private ScalingTimerTask scalingTask;
    private final Timer mTimer = new Timer();

    private boolean mWaving;

    private int mWaveNum;
    private float mRadiusMin;
    private float mRadiusMax;
    private float mWaveInterval;
    private List<Gravity> mGravityList;
    private int mWavingMode;
    @ColorInt
    private int mWaveColor;
    private int mInnerCircleAlpha;
    private int mWaveAlpha;
    private float mInnerCircleScale;
    private Drawable mWaveCenterImage;
    @ColorInt
    private int mWaveCenterImageTint;
    private int mMillisecondsPerFrame;
    private float mDistancePerFrame;
    private PorterDuff.Mode mWaveCenterImageTintMode;
    private Paint mPaint;
    private List<Float> mRadii;

    public WaveView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WaveView);
        mWaveNum = ta.getInteger(R.styleable.WaveView_waveNumber, 3);
        mRadiusMin = ta.getDimension(R.styleable.WaveView_minRadius, -1);
        mRadiusMax = ta.getDimension(R.styleable.WaveView_maxRadius, -1);
        mWaveInterval = ta.getFloat(R.styleable.WaveView_waveInterval, 15f);
        setGravity(ta.getInteger(R.styleable.WaveView_gravity, Gravity.CENTER.getNumVal()));
        mInnerCircleAlpha = ta.getColor(R.styleable.WaveView_innerCircleAlpha, 255);
        mWaveAlpha = ta.getColor(R.styleable.WaveView_waveAlpha, 128);
        mWavingMode = ta.getColor(R.styleable.WaveView_wavingMode, WAVING_MODE_LIMITED);
        mWaveColor = ta.getColor(R.styleable.WaveView_android_color, Color.RED);
        mInnerCircleScale = ta.getFraction(R.styleable.WaveView_innerCircleScale, 1, 1, 0.35f);
        mWaveCenterImage = ta.getDrawable(R.styleable.WaveView_waveCenterImageSrc);
        mWaveCenterImageTint = ta.getColor(R.styleable.WaveView_waveCenterImageTint, -1);
        mWaveCenterImageTintMode = PorterDuffUtils.intToMode(ta.getInteger(R.styleable.WaveView_waveCenterImageTintMode, 0));
        setMillisecondsPerFrame(ta.getInteger(R.styleable.WaveView_millisecondsPerFrame, 16));
        mDistancePerFrame = ta.getFloat(R.styleable.WaveView_distancePerFrame, 0.5f);
        ta.recycle();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mRadii = new ArrayList<>(getWaveNum());
        initRadius();
    }

    public int getWaveNum() {
        return mWaveNum;
    }

    private void initRadius() {
        mRadii.add(getRadiusMin());
        for (int i = 1; i < getWaveNum(); i++) {
            mRadii.add(0f);
        }
    }

    private boolean containsFlag(int flagSet, int flag) {
        return (flagSet | flag) == flagSet;
    }

    public int getShortEdgeLength() {
        int width = getWidth();
        int height = getHeight();
        return Math.min(width, height);
    }

    public void setWaveNum(int waveNum) {
        this.mWaveNum = waveNum;
    }

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getCircleCenterCoordinateX();
        float centerY = getCircleCenterCoordinateY();

        mPaint.setColor(getWaveColor());
        for (float radius : mRadii) {
            mPaint.setAlpha(calcAlpha(radius));
            canvas.drawCircle(centerX, centerY, radius, mPaint);
        }
        mPaint.setAlpha(mInnerCircleAlpha);
        canvas.drawCircle(centerX, centerY, getRadiusMin(), mPaint);
        if (mWaveCenterImage != null) {
            mWaveCenterImage.setBounds((int) (centerX - getRadiusMin()), (int) (centerY - getRadiusMin()), (int) (centerX + getRadiusMin()), (int) (centerY + getRadiusMin()));
            if (mWaveCenterImageTint != -1) {
                mWaveCenterImage.setTint(mWaveCenterImageTint);
                mWaveCenterImage.setTintMode(mWaveCenterImageTintMode);
            }
            mWaveCenterImage.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        int w = widthSpecSize;
        int h = heightSpecSize;

        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            w = h = Math.min((int) DEFAULT_LENGTH_PX, Math.min(widthSpecSize, heightSpecSize));
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            w = heightSpecSize;
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            h = widthSpecSize;
        }
        setMeasuredDimension(w, h);
    }

    private List<Float> transList(List<Float> list) {
        ArrayList<Float> newList = new ArrayList<>(getWaveNum());
        for (int i = 1; i < list.size(); i++) {
            newList.add(list.get(i));
        }
        if (list.size() != 0)
            newList.add(list.get(0));

        return newList;
    }

    private int calcAlpha(float radius) {
        int alpha = (int) ((getRadiusMax() - radius) / (getRadiusMax() - getRadiusMin()) * getWaveAlpha());
        return Math.max(alpha, 0);
    }

    public void startWave() {
        if (!mWaving) {
            scalingTask = new ScalingTimerTask();
            mTimer.schedule(scalingTask, 0, mMillisecondsPerFrame);
            mWaving = true;
        }
    }

    public void stopWave() {
        if (mWaving) {
            scalingTask.cancel();
            resetWave();
            mWaving = false;
        }
    }

    public void forceStopWave() {
        scalingTask.cancel();
    }

    public void resetWave() {
        mRadii.clear();
        initRadius();
        invalidate();
    }

    public float getWaveInterval() {
        return mWaveInterval;
    }

    public void setWaveInterval(float waveInterval) {
        this.mWaveInterval = waveInterval;
    }

    public float getRadiusMax() {
        if (mRadiusMax == -1) {
            switch (mWavingMode) {
                case WAVING_MODE_NON_LIMITED:
                    return (float) ((Math.sqrt(2) * getShortEdgeLength()) / 2);
                case WAVING_MODE_LIMITED:
                default:
                    return getShortEdgeLength() / 2f;
            }
        } else return mRadiusMax;
    }

    public void setRadiusMax(float radiusMax) {
        this.mRadiusMax = radiusMax;
    }

    public float getRadiusMin() {
        if (mRadiusMin == -1) {
            switch (mWavingMode) {
                case WAVING_MODE_NON_LIMITED:
                    return (float) ((Math.sqrt(2) * getShortEdgeLength()) / 2 * mInnerCircleScale);
                case WAVING_MODE_LIMITED:
                default:
                    return getShortEdgeLength() / 2f * mInnerCircleScale;
            }
        } else return mRadiusMin;
    }

    public void setRadiusMin(float mRadiusMin) {
        this.mRadiusMin = mRadiusMin;
        mRadii.clear();
        initRadius();
    }

    public int getWaveColor() {
        return mWaveColor;
    }

    public void setWaveColor(int waveColor) {
        this.mWaveColor = waveColor;
    }

    public void setWaveColor(String colorString) {
        int color = Color.parseColor(colorString);
        setWaveColor(color);
    }

    public int getWaveAlpha() {
        return mWaveAlpha;
    }

    public void setWaveAlpha(int waveAlpha) {
        this.mWaveAlpha = waveAlpha;
    }

    private float getCircleCenterCoordinateX() {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        float x = getWidth() / 2f;
        if (getGravity().contains(Gravity.CENTER_HORIZONTAL) |
                (getGravity().contains(Gravity.INNER_START) & getGravity().contains(Gravity.INNER_END)) |
                (getGravity().contains(Gravity.OUTER_START) & getGravity().contains(Gravity.OUTER_END)) |
                (getGravity().contains(Gravity.INNER_LEFT) & getGravity().contains(Gravity.INNER_RIGHT)) |
                (getGravity().contains(Gravity.OUTER_LEFT) & getGravity().contains(Gravity.OUTER_RIGHT)))
            x = getWidth() / 2f;
        else if (getGravity().contains(Gravity.INNER_LEFT)) x = getRadiusMin() + paddingLeft;
        else if (getGravity().contains(Gravity.INNER_RIGHT))
            x = getWidth() - getRadiusMin() - paddingRight;
        else if (getGravity().contains(Gravity.OUTER_LEFT)) x = getRadiusMax() + paddingLeft;
        else if (getGravity().contains(Gravity.OUTER_RIGHT))
            x = getWidth() - getRadiusMax() - paddingRight;
        else if (isRtl) {
            if (getGravity().contains(Gravity.INNER_START))
                x = getWidth() - getRadiusMin() - paddingRight;
            else if (getGravity().contains(Gravity.INNER_END)) x = getRadiusMin() + paddingLeft;
            else if (getGravity().contains(Gravity.OUTER_START))
                x = getWidth() - getRadiusMax() - paddingRight;
            else if (getGravity().contains(Gravity.OUTER_END)) x = getRadiusMax() + paddingLeft;
        } else if (getGravity().contains(Gravity.INNER_START)) x = getRadiusMin() + paddingLeft;
        else if (getGravity().contains(Gravity.INNER_END))
            x = getWidth() - getRadiusMin() - paddingRight;
        else if (getGravity().contains(Gravity.OUTER_START)) x = getRadiusMax() + paddingLeft;
        else if (getGravity().contains(Gravity.OUTER_END))
            x = getWidth() - getRadiusMax() - paddingRight;
        return x;
    }

    private float getCircleCenterCoordinateY() {
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        float y = getHeight() / 2f;
        if (getGravity().contains(Gravity.CENTER_VERTICAL) |
                (getGravity().contains(Gravity.INNER_TOP) & getGravity().contains(Gravity.INNER_BOTTOM)) |
                (getGravity().contains(Gravity.OUTER_TOP) & getGravity().contains(Gravity.OUTER_BOTTOM)))
            y = getHeight() / 2f;
        else if (getGravity().contains(Gravity.INNER_TOP)) y = getRadiusMin() + paddingTop;
        else if (getGravity().contains(Gravity.INNER_BOTTOM))
            y = getHeight() - getRadiusMin() - paddingBottom;
        else if (getGravity().contains(Gravity.OUTER_TOP)) y = getRadiusMax() + paddingTop;
        else if (getGravity().contains(Gravity.OUTER_BOTTOM))
            y = getHeight() - getRadiusMax() - paddingBottom;
        return y;
    }

    public List<Gravity> getGravity() {
        return mGravityList;
    }

    public void setGravity(int gravityFlagSet) {
        List<Gravity> gravityList = new ArrayList<>();
        for (Gravity gravity : Gravity.values()) {
            if (containsFlag(gravityFlagSet, gravity.getNumVal())) gravityList.add(gravity);
        }
        setGravity(gravityList);
    }

    public void setGravity(List<Gravity> gravityList) {
        mGravityList = gravityList;
    }

    public int getWavingMode() {
        return mWavingMode;
    }

    public void setWavingMode(int wavingMode) {
        mWavingMode = wavingMode;
    }

    public int getInnerCircleAlpha() {
        return mInnerCircleAlpha;
    }

    public void setInnerCircleAlpha(int innerCircleAlpha) {
        mInnerCircleAlpha = innerCircleAlpha;
    }

    public float getInnerCircleScale() {
        return mInnerCircleScale;
    }

    public void setInnerCircleScale(float innerCircleScale) {
        mInnerCircleScale = innerCircleScale;
    }

    public Drawable getWaveCenterImage() {
        return mWaveCenterImage;
    }

    public void setWaveCenterImage(Drawable waveCenterImage) {
        mWaveCenterImage = waveCenterImage;
    }

    public int getWaveCenterImageTint() {
        return mWaveCenterImageTint;
    }

    public void setWaveCenterImageTint(int waveCenterImageTint) {
        mWaveCenterImageTint = waveCenterImageTint;
    }

    public PorterDuff.Mode getWaveCenterImageTintMode() {
        return mWaveCenterImageTintMode;
    }

    public void setWaveCenterImageTintMode(PorterDuff.Mode waveCenterImageTintMode) {
        mWaveCenterImageTintMode = waveCenterImageTintMode;
    }

    public int getMillisecondsPerFrame() {
        return mMillisecondsPerFrame;
    }

    public void setMillisecondsPerFrame(int millisecondsPerFrame) {
        mMillisecondsPerFrame = millisecondsPerFrame >= 8 ? millisecondsPerFrame : 16;
    }

    public float getDistancePerFrame() {
        return mDistancePerFrame;
    }

    public void setDistancePerFrame(float distancePerFrame) {
        mDistancePerFrame = distancePerFrame;
    }

    public enum Gravity {
        CENTER(0x03), CENTER_VERTICAL(0x01), CENTER_HORIZONTAL(0x02),
        INNER_TOP(0x04), INNER_BOTTOM(0x08),
        INNER_START(0x10), INNER_END(0x20),
        INNER_LEFT(0x40), INNER_RIGHT(0x80),
        OUTER_TOP(0x100), OUTER_BOTTOM(0x200),
        OUTER_START(0x400), OUTER_END(0x800),
        OUTER_LEFT(0x1000), OUTER_RIGHT(0x2000);

        private final int numVal;

        Gravity(int numVal) {
            this.numVal = numVal;
        }

        public static Gravity parse(int numVal) {
            for (Gravity o : Gravity.values()) {
                if (o.getNumVal() == numVal) {
                    return o;
                }
            }
            return null;
        }

        public int getNumVal() {
            return numVal;
        }
    }

    class ScalingTimerTask extends TimerTask {
        boolean stopRequested = false;

        @Override
        public void run() {
            for (int i = 0; i < mRadii.size(); i++) {
                float curVal;
                try {
                    curVal = mRadii.get(i);
                } catch (IndexOutOfBoundsException ignored) {
                    continue;
                }
                mRadii.set(i, curVal + mDistancePerFrame);
                if (i < mRadii.size() - 1 & curVal < getRadiusMin() + getWaveInterval()) {
                    mRadii.set(i + 1, getRadiusMin());
                    break;
                }
            }
            if (mRadii.get(0) > getRadiusMax()) {
                if (stopRequested) mRadii.remove(0);
                else mRadii.set(0, 0f);
                List<Float> tempList = transList(mRadii);
                mRadii.clear();
                mRadii.addAll(tempList);
            }

            invalidate();
            if (stopRequested & mRadii.size() == 0) this.cancel();
        }

        public void stop() {
            stopRequested = true;
        }
    }
}