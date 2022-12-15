package amirz.btcodec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;

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
import android.widget.TextView;

import java.util.List;
import java.util.stream.Stream;

@SuppressLint({"MissingPermission", "SetTextI18n"})
public class MainActivity extends AppCompatActivity implements BluetoothProfile.ServiceListener {

    private BluetoothAdapter mAdp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 0);
            setText("Grant permissions then re-open.");
            return;
        }

        mAdp = getSystemService(BluetoothManager.class).getAdapter();
        mAdp.getProfileProxy(this, this, BluetoothProfile.A2DP);
    }

    @Override
    public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
        Log.e("Main", "onConnected");
        BluetoothA2dp a2dp = (BluetoothA2dp) bluetoothProfile;

        List<BluetoothDevice> devs = a2dp.getConnectedDevices();
        if (devs.size() == 0) {
            setText("No device found.");
            return;
        }

        BluetoothDevice dev = devs.get(0);
        String state = stateToString(a2dp.getConnectionState(dev));

        Log.e("Main", "Device: " + dev.getName()
            + " | Connection: " + state
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

        String[] txt = new String[] {
                "Device: " + dev.getName() + "\nState: " + state,
                ">> No change necessary.",
                "Old config:",
                format(oldConfig),
                "New config:",
                format(setConfig),
        };

        if (!setConfig.equals(oldConfig)) {
            Log.e("Main", "SetConfig: " + setConfig);
            a2dp.setCodecConfigPreference(dev, setConfig);
            txt[1] = ">> Updated config.";
        }

        setText(txt);
        mAdp.closeProfileProxy(BluetoothProfile.A2DP, bluetoothProfile);
    }

    private BluetoothCodecConfig getConfig(BluetoothA2dp a2dp) {
        BluetoothDevice dev = a2dp.getConnectedDevices().get(0);
        return a2dp.getCodecStatus(dev).getCodecConfig();
    }

    @Override
    public void onServiceDisconnected(int i){
        Log.e("Main","onDisconnected");
    }

    private void setText(CharSequence... texts) {
        TextView tv = findViewById(R.id.textView);
        tv.setText(String.join("\n\n", texts));
    }

    private String format(BluetoothCodecConfig config) {
        String[] split = config.toString().replace("{", "").replace("}", "").split(",");
        return String.join("\n", Stream.of(split).map(s -> String.join(" = ", s.split(":"))).toArray(String[]::new));
    }

    private static String stateToString(int state) {
        switch (state) {
            case BluetoothA2dp.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothA2dp.STATE_CONNECTING:
                return "Connecting";
            case BluetoothA2dp.STATE_CONNECTED:
                return "Connected";
            case BluetoothA2dp.STATE_DISCONNECTING:
                return "Disconnecting";
            case BluetoothA2dp.STATE_PLAYING:
                return "Playing";
            case BluetoothA2dp.STATE_NOT_PLAYING:
                return "Not Playing";
            default:
                return "<unknown state " + state + ">";
        }
    }
}