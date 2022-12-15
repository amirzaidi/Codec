package amirz.btcodec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity implements BluetoothProfile.ServiceListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 0);
            return;
        }

        BluetoothManager mgr = getSystemService(BluetoothManager.class);
        BluetoothAdapter adp = mgr.getAdapter();
        adp.getProfileProxy(this, this, BluetoothProfile.A2DP);
    }

    @Override
    public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
        Log.e("Main", "onConnected");
        BluetoothA2dp a2dp = (BluetoothA2dp) bluetoothProfile;

        BluetoothDevice dev = a2dp.getConnectedDevices().get(0);
        Log.e("Main", "Device: " + dev.getName()
        + " | Connection: " + (a2dp.getConnectionState(dev) == BluetoothProfile.STATE_CONNECTED)
        + " | Playing: " + a2dp.isA2dpPlaying(dev));

        BluetoothCodecConfig oldConfig = getConfig(a2dp);
        Log.e("Main", "OldConfig: " + oldConfig);

        BluetoothCodecConfig setConfig = new BluetoothCodecConfig.Builder()
        .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
        .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
        .setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_44100)
        .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24)
        .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
        .setCodecSpecific1(1000) // 1000 = Quality, 1003 = Best Effort
        .setCodecSpecific2(0) // 0
        .setCodecSpecific3(0) // 0
        .setCodecSpecific4(0) // 0
        .build();

        Log.e("Main", "SetConfig: " + setConfig);
        a2dp.setCodecConfigPreference(dev, setConfig);
    }

    private BluetoothCodecConfig getConfig(BluetoothA2dp a2dp) {
            List<BluetoothDevice> devices = a2dp.getConnectedDevices();
            BluetoothDevice dev = devices.get(0);
            return a2dp.getCodecStatus(dev).getCodecConfig();
    }

    @Override
    public void onServiceDisconnected(int i){
        Log.e("Main","onDisconnected");
    }
}