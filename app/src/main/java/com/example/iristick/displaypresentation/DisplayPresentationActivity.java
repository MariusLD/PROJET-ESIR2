package com.example.iristick.displaypresentation;

import android.os.Bundle;
import android.view.Display;

import com.example.iristick.BaseActivity;
import com.example.iristick.R;
import com.iristick.smartglass.support.app.HudActivity;
import com.iristick.smartglass.support.app.HudPresentation;

/**
 * This example displays the current time on the display, using standard Android UI elements.
 */
public class DisplayPresentationActivity extends BaseActivity implements HudActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaypresentation_activity);
        getActionBar().setTitle(R.string.displaypresentation_title);
    }

    @Override
    public HudPresentation onCreateHudPresentation(Display display) {
        return new ClockHud(this, display);
    }

}
