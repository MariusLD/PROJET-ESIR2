package com.example.iristick.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.example.iristick.BaseActivity;
import com.example.iristick.R;
import com.iristick.smartglass.core.Headset;
import com.iristick.smartglass.core.VoiceEvent;
import com.iristick.smartglass.support.app.IristickApp;

/**
 * This example opens both Iristick cameras and shows the captured streams.
 *
 * The stream can be zoomed in with pinch-and-zoom and moved by dragging the image.
 * Click on the info text to reset the settings.
 * For the zoom camera, a tap on the image triggers auto-focus..
 */
public class CameraActivity extends BaseActivity {

    /**
     * List of voice commands to register as an enum to easily map recognized command indexes back
     * to the command.
     */
    private enum VoiceCommand {
        FOCUS(R.string.camera_voice_focus),
        ZOOM_IN(R.string.camera_voice_zoom_in),
        ZOOM_OUT(R.string.camera_voice_zoom_out),
        RESET(R.string.camera_voice_reset),
        ;

        static final VoiceCommand[] VALUES = VoiceCommand.values();

        @StringRes
        final int resId;

        VoiceCommand(final int resId) {
            this.resId = resId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        /* To open a camera, the app needs the Android CAMERA permission. */
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Register voice commands. */
        Headset headset = IristickApp.getHeadset();
        if (headset != null) {
            String[] commands = new String[VoiceCommand.VALUES.length];
            for (int i = 0; i < VoiceCommand.VALUES.length; i++)
                commands[i] = getText(VoiceCommand.VALUES[i].resId).toString();
            headset.registerVoiceCommands(commands, mVoiceCallback, null);
        }
    }

    @Override
    protected void onPause() {
        /* Always unregister voice commands in onPause. */
        Headset headset = IristickApp.getHeadset();
        if (headset != null) {
            headset.unregisterVoiceCommands(mVoiceCallback);
        }
        super.onPause();
    }

    private final VoiceEvent.Callback mVoiceCallback = event -> {

    };
}
