package amirz.btcodec;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.stream.Stream;

@SuppressLint({"MissingPermission", "SetTextI18n"})
public class MainActivity extends Activity implements BluetoothProfile.ServiceListener {

    private BluetoothAdapter mAdp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_long);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 0);
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

        CompanionDeviceManager deviceManager = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);

        if (!deviceManager.getAssociations().contains(dev.getAddress())) {
            final AssociationRequest request = new AssociationRequest.Builder()
                    .addDeviceFilter(new BluetoothDeviceFilter.Builder()
                            .setAddress(dev.getAddress())
                            .build())
                    .setSingleDevice(true)
                    .build();

            deviceManager.associate(request, new CompanionDeviceManager.Callback() {
                public void onDeviceFound(IntentSender chooserLauncher) {
                    try {
                        startIntentSenderForResult(chooserLauncher,
                                42, null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("Main", "Error: " + String.valueOf(e));
                    }
                }

                @Override
                public void onFailure(CharSequence error) {
                    Log.e("Main", "Failed to associate device." + " | Error: " + error);
                }
            }, null);
        }

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
                ">> No update necessary.",
                "Old config:",
                format(oldConfig),
                "New config:",
                format(setConfig),
        };

        if (!oldConfig.equals(setConfig)) {
            Log.e("Main", "SetConfig: " + setConfig);
            a2dp.setCodecConfigPreference(dev, setConfig);

            txt[1] = ">> Failed to update config.";

            long st = System.currentTimeMillis();
            while (System.currentTimeMillis() <= st + 5000) {
                if (getConfig(a2dp).equals(setConfig)) {
                    txt[1] = ">> Updated config.";
                    break;
                }
            }
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