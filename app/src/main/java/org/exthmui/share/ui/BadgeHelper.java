package org.exthmui.share.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayout;

import org.exthmui.share.shared.misc.CrossFadeUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

public class BadgeHelper extends View {
    public static final String TAG = "BadgeHelper";
    private float mDensity;
    private final Paint mTextPaint;
    private final Paint mBackgroundPaint;
    @NonNull
    private String mText = "0";

    @Type
    private int mType = Type.TYPE_POINT;
    private boolean mIsOverlap;
    private final RectF mRect = new RectF();
    @ColorInt
    private int mBadgeColor = 0xFFD3321B;
    @ColorInt
    private int mTextColor = 0xFFFFFFff;
    @FloatRange(from = 0f)
    private float mTextSize;
    @IntRange(from = -1)
    private int mWidth = -1;
    @IntRange(from = -1)
    private int mHeight = -1;
    private boolean mIsViewBound;
    private boolean mIgnoreTargetPadding;
    private boolean mIsCenterVertical;
    private int mLeftMargin;
    private int mTopMargin;
    private int mRightMargin;
    private int mBottomMargin;

    @IntDef({Type.TYPE_POINT, Type.TYPE_TEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
        int TYPE_POINT = 0;
        int TYPE_TEXT = 1;
    }

    public BadgeHelper(Context context) {
        super(context);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    private void init(@Type int type, boolean isOverlap) {
        this.mType = type;
        this.mIsOverlap = isOverlap;
        mDensity = getResources().getDisplayMetrics().density;
    }

    public BadgeHelper setBadgeMargins(int left, int top, int right, int bottom) {
        mLeftMargin = left;
        mTopMargin = top;
        mRightMargin = right;
        mBottomMargin = bottom;
        return this;
    }

    public BadgeHelper setBadgeCenterVertical(  ) {
        mIsCenterVertical = true;
        return this;
    }

    /**
     * Set the type of badge
     *
     * @param type {@link Type}
     */
    public BadgeHelper setBadgeType(@Type int type) {
        this.mType = type;
        return this;
    }

    /**
     * Set the size of text
     */
    public BadgeHelper setBadgeTextSize(int textSize) {
        Context c = getContext();
        Resources r;
        if (c == null) {
            r = Resources.getSystem();
        } else {
            r = c.getResources();
        }
        this.mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, r.getDisplayMetrics());
        return this;
    }


    /**
     * Set the color of text
     */
    public BadgeHelper setBadgeTextColor(@ColorInt int textColor) {
        this.mTextColor = textColor;
        return this;
    }

    /**
     * Set the overlap mode, false by default
     *
     * @param isOverlap Whether to make the view overlapped on target view
     */
    public BadgeHelper setBadgeOverlap(boolean isOverlap) {
        this.mIsOverlap = isOverlap;
        return this;
    }

    /**
     * Set whether to ignore the padding of target view, false by default
     *
     * @param isIgnoreTargetPadding Whether to ignore the padding of target view
     */
    public BadgeHelper setIgnoreTargetPadding(boolean isIgnoreTargetPadding) {
        this.mIgnoreTargetPadding = isIgnoreTargetPadding;
        if (!mIsOverlap && isIgnoreTargetPadding) {
            Log.w(TAG, "The ignoreTargetPadding only makes difference in overlap mode");
        }
        return this;
    }

    /**
     * Set the color of the badge itself
     *
     * @param badgeColor The color of badge itself
     */
    public BadgeHelper setBadgeColor(int badgeColor) {
        this.mBadgeColor = badgeColor;
        return this;
    }

    /**
     * Set the size of badge
     *
     * @param width Width
     * @param height Height
     */
    public BadgeHelper setBadgeSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        return this;
    }

    /**
     * Set whether to show the badge
     * @param enabled Whether to show the badge
     */
    public BadgeHelper setBadgeEnabled(boolean enabled) {

        long duration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        if (!mIsViewBound) setVisibility(enabled?VISIBLE:INVISIBLE);
        else {
            if (enabled && getVisibility() != VISIBLE) CrossFadeUtils.fadeIn(this, duration);
            else if (!enabled && getVisibility() == VISIBLE) CrossFadeUtils.fadeOut(this, duration);
        }
        return this;
    }


    /**
     * Set the text of badge
     *
     * @param text The text of badge
     */
    public BadgeHelper setBadgeText(@NonNull String text) {
        this.mText = text;
        if (mIsViewBound) {
            invalidate();
        }
        return this;
    }

    public void bindToTargetView(TabLayout target, int tabIndex) {
        TabLayout.Tab tab = target.getTabAt(tabIndex);
        View targetView = null;
        View tabView = null;
        try {
            @SuppressWarnings("JavaReflectionMemberAccess") Field viewField = TabLayout.Tab.class.getDeclaredField("mView");
            viewField.setAccessible(true);
            targetView = tabView = (View) viewField.get(tab);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (tabView != null) {
                Field mTextViewField = tabView.getClass().getDeclaredField("mTextView");
                mTextViewField.setAccessible(true);
                targetView = (View) mTextViewField.get(tabView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (targetView != null) {
            bindToTargetView(targetView);
        }
    }


    /**
     * Bind badge to the target view
     *
     * @param target Target view
     */
    public void bindToTargetView(View target) {
        init(mType, mIsOverlap);
        if (getParent() != null) {
            ((ViewGroup) getParent()).removeView(this);
        }
        if (target == null) {
            return;
        }
        if (target.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) target.getParent();

            int groupIndex = parent.indexOfChild(target);
            parent.removeView(target);

            if (mIsOverlap) {// Overlap mode
                FrameLayout badgeContainer = new FrameLayout(getContext());
                ViewGroup.LayoutParams targetLayoutParams = target.getLayoutParams();
                badgeContainer.setLayoutParams(targetLayoutParams);

                target.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                parent.addView(badgeContainer, groupIndex, targetLayoutParams);
                badgeContainer.addView(target);
                badgeContainer.addView(this);

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
                if(mIsCenterVertical) {
                    layoutParams.gravity =  Gravity.CENTER_VERTICAL ;
                }else{
                    layoutParams.gravity = Gravity.END | Gravity.TOP;
                }
                if (mIgnoreTargetPadding) {
                    layoutParams.rightMargin = target.getPaddingRight() - mWidth;
                    layoutParams.topMargin = target.getPaddingTop() - mHeight / 2;
                }

                setLayoutParams(layoutParams);
            } else {// Non-overlap mode
                LinearLayout badgeContainer = new LinearLayout(getContext());
                badgeContainer.setOrientation(LinearLayout.HORIZONTAL);
                ViewGroup.LayoutParams targetLayoutParams = target.getLayoutParams();
                badgeContainer.setLayoutParams(targetLayoutParams);

                target.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                parent.addView(badgeContainer, groupIndex, targetLayoutParams);
                badgeContainer.addView(target);
                badgeContainer.addView(this);
                if(mIsCenterVertical) {
                    badgeContainer.setGravity(Gravity.CENTER_VERTICAL);
                }
            }
            boolean hasSetMargin = mLeftMargin >0|| mTopMargin >0|| mRightMargin >0|| mBottomMargin >0;
            if (hasSetMargin&&getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) getLayoutParams();
                p.setMargins(mLeftMargin, mTopMargin, mRightMargin, mBottomMargin);
                setLayoutParams(p);
            }
            mIsViewBound = true;
        } else if (target.getParent() == null) {
            throw new IllegalStateException("Target view MUST have a parent layout!");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWidth >= 0 && mHeight >= 0) {// Size specified
            setMeasuredDimension(mWidth, mHeight);
            return;
        }

        int width = 0,height = 0;
        switch (mType) {
            case Type.TYPE_POINT:
                // Calculate the size of point badge
                width = height = Math.round(getBadgeHeight());
                break;
            case Type.TYPE_TEXT:
                // Calculate the size of text
                if (mTextSize == 0) {
                    mTextPaint.setTextSize(mDensity * 10);
                } else {
                    mTextPaint.setTextSize(mTextSize);
                }

                // Calculate the size of text badge
                width = Math.round(getBadgeWidth());
                height = Math.round(getBadgeHeight());
                break;
        }
        setMeasuredDimension(width, height);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackgroundPaint.setColor(mBadgeColor);
        mTextPaint.setColor(mTextColor);
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = getWidth();
        mRect.bottom = getHeight();
        canvas.drawRoundRect(mRect, getRadius(), getRadius(), mBackgroundPaint);

        if (mType == Type.TYPE_TEXT) {
            float textWidth = getTextWidth(mText, mTextPaint);
            Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
            float distance = (fontMetrics.bottom - fontMetrics.top)/2 - fontMetrics.bottom;
            canvas.drawText(mText, getWidth() / 2f - textWidth / 2, mRect.centerY() + distance, mTextPaint);
        }
    }

    private float getRadius() {
        return getHeight() / 2f;
    }
    private float getBadgeWidth() {
        switch (mType) {
            case Type.TYPE_POINT:
                return mDensity * 7f;
            case Type.TYPE_TEXT:
                return getTextWidth(mText, mTextPaint) + getRadius() * 2;
            default:
                Log.e(TAG, "Invalid type!");
                return 0;
        }
    }
    private float getBadgeHeight() {
        switch (mType) {
            case Type.TYPE_POINT:
                return mDensity * 7f;
            case Type.TYPE_TEXT:
                // Make the background a little bigger than text
                return getTextHeight(mText, mTextPaint) * 1.2f;
            default:
                Log.e(TAG, "Invalid type!");
                return 0;
        }
    }

    private float getTextWidth(String text, Paint p) {
        return p.measureText(text, 0, text.length());
    }

    private float getTextHeight(String text, Paint p) {
        Rect rect = new Rect();
        p.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }
}


