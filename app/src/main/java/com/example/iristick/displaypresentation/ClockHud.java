package com.example.iristick.displaypresentation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.widget.TextView;

import com.example.iristick.R;
import com.iristick.smartglass.support.app.HudPresentation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

class ClockHud extends HudPresentation {

    private TextView mDisplayText;
    private Handler mHandler;
    private final DateFormat mFormat;

    ClockHud(Context outerContext, Display display) {
        super(outerContext, display);
        mHandler = new Handler(getContext().getMainLooper());
        mFormat = SimpleDateFormat.getTimeInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout. */
        setContentView(R.layout.displaypresentation_hud);

        /* Get widgets defined in the layout. */
        mDisplayText = findViewById(R.id.clock);

        /* Start updating the clock. */
        mHandler.postDelayed(mAction, 0);
    }

    @Override
    public void onStop() {
        /* Called when the presentation is dismissed. Do cleanup here. */
        mHandler.removeCallbacks(mAction);
        super.onStop();
    }

    private final Runnable mAction = new Runnable() {
        @Override
        public void run() {
            mDisplayText.setText(mFormat.format(new Date()));
            mHandler.postDelayed(this, 1000);
        }
    };

}
