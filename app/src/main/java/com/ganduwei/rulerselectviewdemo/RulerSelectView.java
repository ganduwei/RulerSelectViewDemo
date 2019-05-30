package com.ganduwei.rulerselectviewdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import java.util.ArrayList;

public class RulerSelectView extends View {

    private final String TAG = "RulerSelectView";

    private final Paint mPaint;
    private final Path mPath;
    private int mDecimalNumber;//进制数
    private float mDecimal;//精确度
    private int mSegmentNumber;//片段数

    private int mScaleLineGap;//刻度线之间的间隔
    private int mScaleLineHeight;//刻度线的高度
    private int mSegmentLineHeight;//片段刻度线的高度
    private int mDecimalLineHeight;//进制刻度线的高度
    private int mScaleLineColor;//刻度线的颜色
    private int mSelectedColor;//中间选中刻度线的颜色
    private boolean mIsShowSelectedScale;//是否显示中间选中刻度线

    private LinearGradient mLinearGradient;
    private boolean mHasGradient;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private final int mScaledMinimumFlingVelocity;
    private final int mScaledMaximumFlingVelocity;

    private int mScaleBottomY;

    private float mLastX = 0;
    private float mOffset = 0;//滑动的距离，每次滑动停止后重置为0
    private float mSnapX;//滑动过程中,中点与距离中点最近的线的长度( cx - x ),会不断变化
    private int mSnapPosition;//滑动过程中,中点与距离最近中点的线的position,会不断变化
    private int mLastSnapPosition = -1;

    private boolean mIsFling = false;//是否是Fling状态

    private ArrayList<Float> mData;
    private int mSelectedPosition = 0;
    private float mSelectedValue;

    private RulerSelectChangeListener mRulerSelectChangeListener;
    private float mMaxLeftOffset;
    private float mMaxRightOffset;

    public interface RulerSelectChangeListener {
        void onRulerChange(RulerSelectView rulerSelectView, int selectedPosition, float selectedValue);
    }

    public RulerSelectView(Context context) {
        this(context, null);
    }

    public RulerSelectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RulerSelectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPath = new Path();
        mDecimalNumber = 10;
        mDecimal = 10;
        mSegmentNumber = 5;
        mIsShowSelectedScale = true;
        mHasGradient = true;
        final int gap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, getResources().getDisplayMetrics());
        mScaleLineGap = gap;
        mScaleLineHeight = gap * 3;
        mSegmentLineHeight = gap * 4;
        mDecimalLineHeight = mSegmentLineHeight;
        mScaleLineColor = Color.GRAY;
        mSelectedColor = Color.RED;

        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(gap);
        mPaint.setStrokeWidth(gap / 10f);

        mScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mScaledMinimumFlingVelocity = vc.getScaledMinimumFlingVelocity();//150
        mScaledMaximumFlingVelocity = vc.getScaledMaximumFlingVelocity();//24000
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (widthMode != MeasureSpec.EXACTLY) {
            width = displayMetrics.widthPixels;
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, displayMetrics);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Rect rect = new Rect();
        mPaint.getTextBounds("1", 0, 1, rect);
        int textHeight = rect.height();
        mScaleBottomY = h - getPaddingBottom() - textHeight - mScaleLineGap;
        if (mHasGradient) {
            mLinearGradient = new LinearGradient(0f, 0f, w / 2f, 0f,
                    new int[]{Color.TRANSPARENT, mScaleLineColor},
                    new float[]{0.2f, 0.8f},
                    Shader.TileMode.MIRROR);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mData == null || mData.isEmpty()) return;
        final int w = getWidth();
        final int h = getHeight();
        if (w == 0 || h == 0) return;
        mPath.reset();

        final int size = mData.size();
        float cx = w / 2f;//中点
        float x;
        float snap;

        //重置
        mSnapX = Integer.MAX_VALUE;

        if (mHasGradient) {
            mPaint.setShader(mLinearGradient);
        } else {
            mPaint.setColor(mScaleLineColor);
        }
        mPaint.setStyle(Paint.Style.FILL);

        //两个循环，一个从 mSelectedPosition 往上循环，另一个从 mSelectedPosition 往下开始循环
        for (int i = mSelectedPosition; i < size; i++) {
            x = cx + (i - mSelectedPosition) * mScaleLineGap + mOffset;
            if (x > w - getPaddingEnd()) {
                break;
            }
            //在往左的fling状态下(很快),由于mSelectedPosition没有及时改变,会有很多大于mSelectedPosition的i的坐标小于w - getPaddingEnd(),并且小于 getPaddingStart
            if (x < getPaddingStart()) {
                continue;
            }
            snap = cx - x;
            forPath(canvas, i, x, snap, h);
        }
        if (mSelectedPosition > 0) {
            for (int i = mSelectedPosition - 1; i >= 0; i--) {
                x = cx - (mSelectedPosition - i) * mScaleLineGap + mOffset;
                if (x < getPaddingStart()) {
                    break;
                }
                //在往右的fling状态下(很快),由于mSelectedPosition没有及时改变,会有很多小于mSelectedPosition的i的坐标大于getPaddingEnd,并且大于w - getPaddingEnd
                //此种情况只能使用continue跳过而不能break，因为要绘制的i比较小,直接返回会导致屏幕上什么都不绘制;
                //如果不加这个判断continue,会导致绘制很多(几百个),出现虽然绘制了，但是屏幕上还是什么都不显示的情况(只出现绘制的文字).
                if (x > w - getPaddingEnd()) {
                    continue;
                }
                snap = cx - x;
                forPath(canvas, i, x, snap, h);
            }
        }
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mPath, mPaint);
        mPath.reset();

        if (mHasGradient) {
            mPaint.setShader(null);
        }

        if (mIsShowSelectedScale) {
            mPath.moveTo(cx, mScaleBottomY);
            mPath.lineTo(cx, mScaleBottomY - mScaleLineHeight - 2 * mScaleLineGap);//TODO线的高度
            mPaint.setColor(mSelectedColor);
            canvas.drawPath(mPath, mPaint);
        }

        if (mLastSnapPosition != mSnapPosition) {
            mLastSnapPosition = mSnapPosition;
            mSelectedValue = Math.round(mData.get(mSnapPosition) * mDecimal) / mDecimal;
            if (mRulerSelectChangeListener != null) {
                mRulerSelectChangeListener.onRulerChange(this, mSnapPosition, mSelectedValue);
            }
        }
    }

    private void forPath(Canvas canvas, int i, float x, float snap, int h) {
        if (Math.abs(mSnapX) > Math.abs(snap)) {//滑动过程中，与中点距离最短
            mSnapX = snap;
            if (mSnapPosition != i) mSnapPosition = i;
        }
        mPath.moveTo(x, mScaleBottomY);
        if (i % mDecimalNumber == 0) {
            mPath.lineTo(x, mScaleBottomY - mDecimalLineHeight);
            float s = Math.round(mData.get(i) * mDecimal) / mDecimal;
            String text;
            if (s * mDecimalNumber % mDecimalNumber == 0) {
                text = String.valueOf((int) s);
            } else {
                text = String.valueOf(s);
            }
            canvas.drawText(text, x, h - getPaddingBottom(), mPaint);
        } else if (i % mSegmentNumber == 0) {
            mPath.lineTo(x, mScaleBottomY - mSegmentLineHeight);
        } else {
            mPath.lineTo(x, mScaleBottomY - mScaleLineHeight);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mData == null || mData.isEmpty()) return super.onTouchEvent(event);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        // 只在MotionEvent.ACTION_UP里面添加是不起作用的
        // 翻译：向速率追踪者中加入一个用户的移动事件，你应该最先在ACTION_DOWN调用这个方法，
        // 然后在你接受的ACTION_MOVE，最后是ACTION_UP。你可以为任何一个你愿意的事件调用该方法
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //在快速滑动过程中,ViewPager会直接拦截事件自己处理，所以这里判断如果不是边界直接请求不要拦截
                if (!isRightBound() && !isLeftBound()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (mScroller.computeScrollOffset()) {
                    mScroller.forceFinished(true);
                }
                mIsFling = false;
                mLastX = event.getX();

                mMaxLeftOffset = mSelectedPosition * mScaleLineGap;
                mMaxRightOffset = (mSelectedPosition - mData.size() + 1) * mScaleLineGap;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float dx = x - mLastX;
                boolean isLeftBound = isLeftBound();
                boolean isRightBound = isRightBound();
                if (isLeftBound || isRightBound) {
                    if (dx == 0 || (isLeftBound && dx > 0) || (isRightBound && dx < 0)) {
                        return false;
                    }
                }
                //如果到达了边界，但是滑动方向与边界方向相反，仍然可以滑动，所以这里也需要请求不要拦截
                getParent().requestDisallowInterceptTouchEvent(true);//请求不要拦截
                if (dx == 0) return true;
                mOffset += dx;
                mLastX = x;
                if (dx > 0) {
                    if (mOffset > mMaxLeftOffset) {
                        mOffset = mMaxLeftOffset;
                    }
                } else {
                    if (mOffset < mMaxRightOffset) {
                        mOffset = mMaxRightOffset;
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //参数：units 你想要指定的得到的速度单位，如果值为1，代表1毫秒运动了多少像素。
                //     如果值为1000，代表1秒内运动了多少像素。
                //     同样的力量，units 的数值越大，滑动的距离越长，数值越小，滑动的距离越小
                mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
                //手指往左,velocityX 和 finalX 为负数； 手指往右,velocityX 和 finalX 为正数
                float velocityX = mVelocityTracker.getXVelocity();
                if (Math.abs(velocityX) > mScaledMinimumFlingVelocity) {
                    if ((isLeftBound() && velocityX > 0) || (isRightBound() && velocityX < 0)) {
                        mLastX = 0;
                        mVelocityTracker.clear();
                        break;
                    }
                    mIsFling = true;
                    mScroller.fling(0, 0, (int) velocityX, 0,
                            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    mOffset = Math.round(mOffset);
                } else {//没有触发filing ，调整位置
                    mScroller.startScroll(0, 0, (int) mSnapX, 0, 300);
                }
                invalidate();
                mLastX = 0;
                mVelocityTracker.clear();
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            final int currX = mScroller.getCurrX();
            final int finalX = mScroller.getFinalX();
            //手指往右，限制左边界
            if (mIsFling && finalX > 0 && mOffset > mMaxLeftOffset) {
                mScroller.forceFinished(true);
                mSelectedPosition = 0;
                mOffset = 0;
                mIsFling = false;
                invalidate();
                return;
            }
            //手指往左，限制右边界
            if (mIsFling && finalX < 0 && mOffset < mMaxRightOffset) {
                mScroller.forceFinished(true);
                mSelectedPosition = mData.size() - 1;
                mOffset = 0;
                mIsFling = false;
                invalidate();
                return;
            }
            final int remain = Math.abs(finalX - currX);
            if (mIsFling && remain <= mScaleLineGap) {
                int dx = mScaleLineGap;
                //mold与mOffset正负号一致
                int mold = Math.round(mOffset % dx);
                //finalX 和 mOffset 可能会不是同一方向的(手指往左滑，mOffset为负，释放时却是往右，finalX为正数)
                if (finalX > 0) {//dx必须为正数，与滑动方向才一致
                    dx -= mold;
                } else {//dx必须为负数，与滑动方向才一致
                    dx = -dx - mold;
                }
                //dx正数相当于手指往右滑动，dx负数相当于手指往左滑动
                mIsFling = false;
                mLastX = 0;
                mScroller.forceFinished(true);
                mScroller.startScroll(0, 0, dx, 0, 180);
                invalidate();
                return;
            }
            mOffset = mOffset + currX - mLastX;
            mLastX = currX;
            if (mScroller.isFinished()) {
                mSelectedPosition = mSnapPosition;
                mOffset = 0;
            }
            invalidate();
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        if (mSelectedPosition != 0) {
            Bundle bundle = new Bundle();
            Parcelable parcelable = super.onSaveInstanceState();
            bundle.putParcelable("sys", parcelable);
            bundle.putInt("i", mSelectedPosition);
            return bundle;
        } else {
            return super.onSaveInstanceState();
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mSelectedPosition = bundle.getInt("i");
            mSelectedValue = mData.get(mSelectedPosition);
            super.onRestoreInstanceState(bundle.getParcelable("sys"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * @param start         刻度开始值
     * @param end           刻度结束值
     * @param decimalNumber 小数进制（数值1分为多少份，默认是10）
     * @param segmentNumber 片段（高低刻度分割的片段） 默认为5，表示一个片段包含多少个刻度
     */
    public void setRulerRange(float start, float end, int decimalNumber, int segmentNumber) {
        this.mDecimalNumber = decimalNumber;
        this.mSegmentNumber = segmentNumber;
        setRulerRange(start, end);
    }

    /**
     * @param start 刻度开始值
     * @param end   刻度结束值
     */
    public void setRulerRange(float start, float end) {
        float diff = end - start;
        if (diff <= 0) {
            throw new IllegalArgumentException("the start must less than the end");
        }
        if (mData == null) {
            int initialCapacity = (int) (diff * mDecimalNumber) + 1;
            mData = new ArrayList<>(initialCapacity);
        } else {
            mData.clear();
        }
        float d = 1f / mDecimalNumber;
        end = end + d / 10f;
        for (float i = start; i < end; i += d) {
            mData.add(i);
        }
        invalidate();
    }

    /**
     * @param decimalCount 小数位的个数
     */
    public void setDecimalCount(int decimalCount) {
        mDecimal = (int) Math.pow(10, decimalCount);
    }

    public void setSelectedPosition(int selectedPosition) {
        if (mSelectedPosition == selectedPosition) return;
        if (mData != null && !mData.isEmpty()) {
            if (selectedPosition < 0) {
                mSelectedPosition = 0;
            } else if (selectedPosition > mData.size() - 1) {
                mSelectedPosition = mData.size() - 1;
            } else {
                mSelectedPosition = selectedPosition;
            }
            mSelectedValue = mData.get(mSelectedPosition);
            invalidate();
        } else {
            Log.w(TAG, "setSelectedPosition invalid , call setRulerRange first !");
        }
    }

    public void setSelectedValue(float selectedValue) {
        if (selectedValue == mSelectedValue) return;
        if (mData != null && !mData.isEmpty()) {
            int i = findPositionByValue(selectedValue);
            mSelectedPosition = i;
            mSelectedValue = mData.get(i);
            invalidate();
        } else {
            Log.w(TAG, "setSelectedValue invalid , call setRulerRange first !");
        }
    }

    public float getSelectedValue() {
        return mSelectedValue;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    /**
     * @param rulerSelectChangeListener 刻度变化监听
     */
    public void setRulerSelectChangeListener(RulerSelectChangeListener rulerSelectChangeListener) {
        this.mRulerSelectChangeListener = rulerSelectChangeListener;
    }

    /**
     * @param scaleTextSize 刻度线数值文字尺寸
     */
    public void setScaleTextSize(int scaleTextSize) {
        mPaint.setTextSize(scaleTextSize);
    }

    /**
     * @param scaleLineWidth 刻度线宽度
     */
    public void setScaleLineWidth(int scaleLineWidth) {
        mPaint.setStrokeWidth(scaleLineWidth);
    }

    /**
     * @param scaleLineGap 刻度线间隔
     */
    public void setScaleLineGap(int scaleLineGap) {
        mScaleLineGap = scaleLineGap;
    }

    /**
     * @param scaleLineColor 刻度线颜色
     */
    public void setScaleLineColor(int scaleLineColor) {
        mScaleLineColor = scaleLineColor;
    }

    /**
     * @param selectedColor 选中刻度线颜色
     */
    public void setSelectedColor(int selectedColor) {
        mSelectedColor = selectedColor;
    }

    /**
     * @param scaleLineHeight 刻度线高度
     */
    public void setScaleLineHeight(int decimalLineHeight, int segmentLineHeight, int scaleLineHeight) {
        mDecimalLineHeight = decimalLineHeight;
        mSegmentLineHeight = segmentLineHeight;
        mScaleLineHeight = scaleLineHeight;
    }

    /**
     * @param isShow 是否显示选中的刻度
     */
    public void showSelectedScale(boolean isShow) {
        mIsShowSelectedScale = isShow;
    }

    /**
     * @param hasGradient 是否有渐变效果
     */
    public void hasGradient(boolean hasGradient) {
        mHasGradient = hasGradient;
    }

    private int findPositionByValue(float value) {
        if (value <= mData.get(0)) {
            return 0;
        } else if (value >= mData.get(mData.size() - 1)) {
            return mData.size() - 1;
        } else {
            float first = mData.get(0);
            return Math.round((value - first) * mDecimalNumber);
        }
    }

    private boolean isLeftBound() {
        return mSnapPosition == 0 && mSnapX == 0;
    }

    private boolean isRightBound() {
        return mSnapPosition == mData.size() - 1 && mSnapX == 0;
    }
}
