package amirz.btcodec;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

@SuppressLint({"MissingPermission", "SetTextI18n"})
public class MainActivity extends Activity {

    private BPListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_long);
        mListener = new BPListener(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[] {
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                },
                0
            );
            setText("Grant permissions then re-open.");
        } else {
            mListener.pair(this);
        }
    }

    public void setText(CharSequence... texts) {
        TextView tv = findViewById(R.id.textView);
        tv.setText(String.join("\n\n", texts));
    }
}