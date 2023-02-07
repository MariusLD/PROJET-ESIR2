package com.example.iristick.touchpad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import com.example.iristick.R;
import com.iristick.smartglass.core.TouchEvent;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Simple widget to flash an icon corresponding to a gesture.
 */
public class GestureView extends AppCompatImageView {

    public GestureView(Context context) {
        super(context);
    }

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void showGesture(int gestureCode) {
        int resId;
        switch (gestureCode) {
        case TouchEvent.GESTURE_SWIPE_FORWARD:  resId = R.drawable.gesture_swipe_forward;   break;
        case TouchEvent.GESTURE_SWIPE_BACKWARD: resId = R.drawable.gesture_swip_backward;   break;
        case TouchEvent.GESTURE_SWIPE_DOWN:     resId = R.drawable.gesture_swipe_down;      break;
        case TouchEvent.GESTURE_TAP:            resId = R.drawable.gesture_tap;             break;
        case TouchEvent.GESTURE_DOUBLE_TAP:     resId = R.drawable.gesture_double_tap;      break;
        case TouchEvent.GESTURE_LONG_TAP:       resId = R.drawable.gesture_long_tap;        break;
        default:
            return;
        }
        setImageDrawable(getResources().getDrawable(resId, null));
        setAlpha(1.0f);
        animate().alpha(0.0f).setDuration(750).setInterpolator(new DecelerateInterpolator());
    }

}
