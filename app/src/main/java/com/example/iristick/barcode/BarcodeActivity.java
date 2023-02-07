package com.example.iristick.barcode;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.iristick.BaseActivity;
import com.iristick.smartglass.core.Intents;
import com.example.iristick.R;

/**
 * This example scans for a barcode using intents provided by the Iristick Services.
 */
public class BarcodeActivity extends BaseActivity {

    private static final int REQUEST_CODE = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcode_activity);
    }

    public void doScanAny(View view) {
        /* The user clicked on the "Scan any barcode" button. */
        Intent intent = new Intent(Intents.ACTION_SCAN_BARCODE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void doScanQr(View view) {
        /* The user clicked on the "Scan QR code" button. */
        Intent intent = new Intent(Intents.ACTION_SCAN_BARCODE);
        /* You can set the barcode formats to search for with a comma-separated string. */
        intent.putExtra(Intents.EXTRA_BARCODE_SCAN_FORMATS, "QR_CODE");
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            switch (resultCode) {
            case Intents.RESULT_OK:
                /* A barcode was found. */
                String text = data.getStringExtra(Intents.EXTRA_BARCODE_RESULT);
                String format = data.getStringExtra(Intents.EXTRA_BARCODE_FORMAT);
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.barcode_result, format, text))
                        .setNeutralButton(R.string.ok, null)
                        .show();
                break;
            case Intents.RESULT_CANCELLED:
                /* Cancelled by the user. */
                break;
            default:
                /* Another error occurred. */
                Toast.makeText(this, getString(R.string.barcode_error, resultCode), Toast.LENGTH_LONG).show();
            }
        }
    }

}
