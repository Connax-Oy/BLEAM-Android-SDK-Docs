package io.connax.bleamsdktest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;

import com.google.android.material.button.MaterialButton;

import io.connax.bleam.BleamSDK;
import io.connax.bleamsdktest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSIONS_REQUEST = 42;
    ActivityMainBinding binding;
    private BleamSDK sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // TODO replace appId and appSecret with real data
        sdk = BleamSDK.getInstance(
                this,
                "appId",
                "appSecret");

        binding.geoButton.setText(sdk.isGeofencingEnabled() ? "Disable Geofencing" : "Enable Geofencing");

        // Subscribing to broadcasts to show in logs on Activity
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleamSDK.ACTION_BLEAM);
        filter.addAction(BleamSDK.ACTION_GEOFENCE);
        filter.addAction(BleamSDK.ACTION_LOGS);
        registerReceiver(receiver, filter);

        // Checking location permission
        // You can use your own realisation of permission requests
        // Just make sure location is enabled before using BLEAM SDK
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void showDialog(String message, DialogInterface.OnClickListener onClickListener) {
        try {
            new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton("OK", onClickListener)
                    .setCancelable(false)
                    .create().show();
        } catch (Exception e) {
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showDialog("Fine location is needed", (dialogInterface, i) -> requestPermissions());
            } else {
                requestPermissions();
            }
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    PERMISSIONS_REQUEST
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showDialog("Bye then", ((dialogInterface, i) -> finish()));
            }
        }
    }

    public void onGeoClick(View view) {
        if (sdk.isGeofencingEnabled()) {
            sdk.disableGeofencing();
            binding.geoButton.setText("Enable Geofencing");
        } else {
            sdk.enableGeofencing();
            binding.geoButton.setText("Disable Geofencing");
        }
    }

    public void onBleamAuto(View view) {
        // Start BLEAM by GPS location
        sdk.startBleam();
    }

    public void onBleamManual(View view) {
        // Start BLEAM manually using geofence's External ID
        sdk.startBleam(binding.extIdEdit.getText().toString());
    }

    public void onLogs(String logs) {
        binding.logsView.setText(binding.logsView.getText() + "\n" + logs);
        binding.scrollView.post(() -> binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BleamSDK.ACTION_BLEAM:
                    if (intent.getBooleanExtra(BleamSDK.EXTRA_SUCCESS, false)) {
                        onBleamSuccess(intent.getStringExtra(BleamSDK.EXTRA_EXTERNAL_ID),
                                intent.getIntExtra(BleamSDK.EXTRA_POSITION, -1));
                    } else {
                        onBleamFailure(intent.getIntExtra(BleamSDK.EXTRA_ERROR_CODE, -1));
                    }
                    break;
                case BleamSDK.ACTION_GEOFENCE:
                    onGeofenceEnter(intent.getStringExtra(BleamSDK.EXTRA_EXTERNAL_ID));
                    break;
                case BleamSDK.ACTION_LOGS:
                    onLogs(intent.getStringExtra(BleamSDK.EXTRA_LOGS));
            }
        }
    };

    private void onBleamSuccess(String extId, int position) {
        onLogs("BLEAM finished on geo " + extId + ", position: " + position);
    }

    private void onBleamFailure(int error) {
        onLogs("BLEAM has encountered error:");
        switch (error) {
            case BleamSDK.ERROR_WRONG_APP_ID_SECRET_OR_GEOFENCE:
                onLogs("Wrong application ID, secret or geofence ID");
                break;
            case BleamSDK.ERROR_NO_TF_MODEL:
                onLogs("Geofence has no approved model");
                break;
            case BleamSDK.ERROR_SERVER_CONNECTION:
                onLogs("No connection to server");
                break;
            case BleamSDK.ERROR_DEVICE_NOT_SUPPORTED:
                onLogs("Device not supported");
                break;
            case BleamSDK.ERROR_BLUETOOTH_NOT_ENABLED:
                onLogs("Bluetooth disabled");
                break;
            case BleamSDK.ERROR_NOT_IN_GEOFENCE:
                onLogs("Device is not in geofence");
                break;
            case BleamSDK.ERROR_LOCATION_DISABLED:
                onLogs("No location permission or location is disabled");
                break;
            default:
                onLogs("Unknown error");
        }
    }

    private void onGeofenceEnter(String extId) {
        onLogs("Entered geofence " + extId);
    }
}
