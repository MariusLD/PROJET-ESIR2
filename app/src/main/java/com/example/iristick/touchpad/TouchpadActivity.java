package com.example.iristick.touchpad;

import android.os.Bundle;
import android.view.MotionEvent;

import com.example.iristick.BaseActivity;
import com.example.iristick.R;
import com.iristick.smartglass.core.Headset;
import com.iristick.smartglass.core.TouchEvent;
import com.iristick.smartglass.support.app.IristickApp;

/**
 * This example shows how to use the touchpad to provide a drawing surface
 * The drawing surface has two modes:
 *   In cursor mode, the drawing pencil is moved without drawing by touching the touchpad.
 *   In draw mode, the drawing pencil will draw when moved around.
 *   Long tap switches mode.
 *   Tap erases the last line.
 *   Double tap erases the entire drawing.
 */
public class TouchpadActivity extends BaseActivity implements TouchEvent.Callback {

    private GestureView mGesture;
    private DrawingArea mDrawing;
    private float mLastX;
    private float mLastY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.touchpad_activity);
        getActionBar().setTitle(R.string.touchpad_title);
        mGesture = findViewById(R.id.gesture);
        mDrawing = findViewById(R.id.drawing);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Headset headset = IristickApp.getHeadset();
        if (headset != null)
            headset.registerTouchEventCallback(this, null, Headset.TOUCHPAD_FLAG_OVERRIDE_ALL);
    }

    @Override
    protected void onPause() {
        Headset headset = IristickApp.getHeadset();
        if (headset != null)
            headset.unregisterTouchEventCallback(this);
        super.onPause();
    }

    @Override
    public void onTouchEvent(TouchEvent event) {
        /*
         * This method is called when the user interacts with the touchpad.
         * A TouchEvent always has a gesture code, and may have an optional MotionEvent object.
         *
         * The following gesture codes can be reported:
         *
         * - GESTURE_ENTER
         *   The user has started touching the touchpad.
         *
         * - GESTURE_EXIT
         *   The user has stopped touching the touchpad.
         *
         * - GESTURE_SWIPE_FORWARD
         *   The user has made a forward horizontal swipe gesture, sliding the finger in the user's
         *   looking direction.
         *
         * - GESTURE_SWIPE_BACKWARD
         *   The user has made a backward horizontal swipe gesture, sliding the finger towards the back
         *   of the user's head.
         *
         * - GESTURE_SWIPE_DOWN
         *   The user has mad a downward vertical swipe gesture, sliding the finger towards the ground.
         *
         * - GESTURE_TAP
         *   The user has tapped the touchpad shortly once.  Note that this gesture is reported with a
         *   slight delay to distinguish between a single and a double tap.  If you do not need double taps,
         *   you may want to look for GESTURE_TAP_UNCONFIRMED instead.
         *
         * - GESTURE_TAP_UNCONFIRMED
         *   The user has tapped the touchpad shortly.  This gesture is reported as soon as a tap is
         *   detected without delay.  Hence, it is also reported for the first tap in a double tap
         *   sequence.  It is not reported for the second tap of a double tap.
         *
         * - GESTURE_DOUBLE_TAP
         *   The user has tapped the touchpad twice in a rapid succession.
         *
         * - GESTURE_LONG_TAP
         *   The user is touching the touchpad for an extended amount of time without moving.
         *
         * - GESTURE_NONE
         *   No gesture detected.
         *
         * Fine-grained finger movement may be reported through a MotionEvent object.
         * Some hardware may not support fine-grained reporting, so you should always check that the
         * returned MotionEvent object is not null before using it.
         * The following actions are reported:
         *
         * - ACTION_DOWN
         *   The user has started touching the touchpad (corresponds to GESTURE_ENTER).
         *
         * - ACTION_MOVE
         *   The user's finger is moving on the touchpad.
         *
         * - ACTION_UP
         *   The user's finger has been lifted from the touchpad (corresponds to GESTURE_EXIT).
         *
         * - ACTION_HOVER_MOVE
         *   This action is reported when a gesture has been recognized after the user has stopped touching
         *   the touchpad (e.g., a delayed GESTURE_TAP).  A MotionEvent is still provided to get the X-Y
         *   coordinates of such events.  In most cases however you can simply ignore all MotionEvents with
         *   this action code.
         */
        mGesture.showGesture(event.getGestureCode());
        switch (event.getGestureCode()) {
        case TouchEvent.GESTURE_LONG_TAP:
            mDrawing.switchMode();
            break;
        case TouchEvent.GESTURE_TAP:
            mDrawing.removeLast();
            break;
        case TouchEvent.GESTURE_DOUBLE_TAP:
            mDrawing.clearDrawing();
            break;
        }

        if (event.getMotionEvent() != null) {
            float newX = event.getMotionEvent().getX();
            float newY = event.getMotionEvent().getY();

            switch (event.getMotionEvent().getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDrawing.startLine();
                break;
            case MotionEvent.ACTION_MOVE:
                mDrawing.movePencil((newX - mLastX) * 600, (mLastY - newY) * 600);
                break;
            }
            mLastX = newX;
            mLastY = newY;
        }
    }

}
