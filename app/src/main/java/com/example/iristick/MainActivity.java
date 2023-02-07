package com.example.iristick;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;


import androidx.annotation.Nullable;

import com.iristick.smartglass.core.Headset;
import com.iristick.smartglass.core.IristickBinding;
import com.iristick.smartglass.core.IristickConnection;
import com.iristick.smartglass.support.app.IristickApp;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        ListView list = findViewById(R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchExample(Example.values()[position]);
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(IristickApp.wrapContext(newBase));
    }

    @Override
    protected void onStart() {
        super.onStart();
        IristickApp.registerConnectionListener(mIristickListener, null);
    }

    @Override
    protected void onStop() {
        IristickApp.unregisterConnectionListener(mIristickListener);
        super.onStop();
    }

    private void showError(String message) {
        ((TextView) findViewById(R.id.error)).setText(message);
        findViewById(R.id.error_layout).setVisibility(View.VISIBLE);
    }

    private void launchExample(Example example) {
        startActivity(new Intent(this, example.activity));
    }

    private final ListAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return Example.values().length;
        }

        @Override
        public Example getItem(int position) {
            return Example.values()[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null)
                view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.main_item, parent, false);
            Example example = getItem(position);
            ((TextView) view.findViewById(R.id.title)).setText(example.title);
            ((TextView) view.findViewById(R.id.description)).setText(example.description);
            return view;
        }
    };

    /* Check for Iristick connection with phone. */
    private final IristickConnection mIristickListener = new IristickConnection() {
        @Override
        public void onHeadsetConnected(Headset headset) {
            /*
             * This method is called when a headset is connected.
             * In the context of an Activity (like here), we can ignore this event as the activity
             * will be destroyed and recreated whenever a headset is connected or disconnected.
             */
        }

        @Override
        public void onHeadsetDisconnected(Headset headset) {
            /*
             * This method is called when a headset is disconnected.
             * In the context of an Activity (like here), we can ignore this event as the activity
             * will be destroyed and recreated whenever a headset is connected or disconnected.
             */
        }

        @Override
        public void onIristickServiceInitialized(IristickBinding binding) {
            /*
             * This method is called when the app has successfully made a connection
             * with the Iristick Services.  At this point, you can check whether
             * Iristick smart glasses are connected with IristickApp.getHeadset().
             */
            if (IristickApp.getHeadset() == null)
                showError(getString(R.string.error_no_headset));
        }

        @Override
        public void onIristickServiceError(int error) {
            /*
             * This method is called if the app could not connect to the Iristick Services.
             * At this point, the Iristick SDK is NOT functional.
             *
             * The cause of the error is given as argument:
             *
             * - ERROR_NOT_INSTALLED
             *   Iristick Services are not installed on the user's phone. You may want to redirect the user to
             *   the Play Store to install the Iristick Services.
             *
             * - ERROR_NOT_ALLOWED
             *   A policy forbids your app to use Iristick smart glasses. Currently, all apps are allowed to
             *   connect to the Iristick Services, and this code is never used. It may be used in the future.
             *
             * - ERROR_FUTURE_SDK
             *   The Iristick Services installed on the user's phone is older than the SDK with which this app
             *   has been built. As newer SDKs may make use of new features not available in older versions of
             *   the Services, this is not allowed. You should ask the user to upgrade the Iristick Services.
             *
             * - ERROR_DEPRECATED_SDK
             *   The SDK with which this app has been built is not supported anymore by the Iristick Services
             *   installed on the user's phone. You will likely never see this error code, as updates to the
             *   Iristick Services try very hard to be backwards compatible with older SDKs. Should you get
             *   this code, you may ask the user to upgrade to a newer version of your app that is built with
             *   a newer Iristick SDK. Alternatively, the user can also downgrade the Iristick Services.
             *
             * - ERROR_UNKNOWN
             *   An unknown error has occurred that does not fit any case above. This may be due to a new
             *   version of the Iristick Services introducing new error codes that are not understood by
             *   the SDK with which this app is built. Make sure to build the app with the latest SDK.
             *   If you still get an unknown error, please send a logcat to support@iristick.com.
             */
            switch (error) {
            case IristickConnection.ERROR_NOT_INSTALLED:
                showError(getString(R.string.error_not_installed));
                break;
            case IristickConnection.ERROR_FUTURE_SDK:
                showError(getString(R.string.error_future_sdk));
                break;
            case IristickConnection.ERROR_DEPRECATED_SDK:
                showError(getString(R.string.error_deprecated_sdk));
                break;
            default:
                showError(getString(R.string.error_other, error));
            }
        }
    };

}
