package com.example.iristick.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Simple custom view overriding a TextureView and providing gestures.
 */
public class CameraPreview extends TextureView
        implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    public interface OnGestureListener {
        void onZoom(float factor);
        void onPan(int dx, int dy);
    }

    private OnGestureListener mOnGestureListener;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    public CameraPreview(Context context) {
        super(context);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOpaque(false);
        mGestureDetector = new GestureDetector(getContext(), this);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
    }

    public void setOnGestureListener(OnGestureListener listener) {
        mOnGestureListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = mScaleGestureDetector.onTouchEvent(event);
        handled = mGestureDetector.onTouchEvent(event) || handled;
        return handled;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
        case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
            if (mOnGestureListener != null)
                mOnGestureListener.onZoom(2.0f);
            return true;
        case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
            if (mOnGestureListener != null)
                mOnGestureListener.onZoom(0.5f);
            return true;
        default:
            return super.performAccessibilityAction(action, arguments);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return performClick();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mOnGestureListener != null)
            mOnGestureListener.onPan((int)distanceX, (int)distanceY);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mOnGestureListener != null)
            mOnGestureListener.onZoom(detector.getScaleFactor());
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

}
