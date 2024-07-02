package amirz.btcodec;

import android.annotation.SuppressLint;
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
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class BPListener implements BluetoothProfile.ServiceListener {
    private static final int PLAY_DELAY = 1500;

    private final Context mContext;
    private final BluetoothAdapter mAdp;

    private MainActivity mActivity;
    private int mRate = -1;
    private MediaController mMc;

    public BPListener(Context context) {
        mContext = context;
        mAdp = context.getSystemService(BluetoothManager.class).getAdapter();
    }

    public void pair(MainActivity activity) {
        mActivity = activity;
        mAdp.getProfileProxy(mContext, this, BluetoothProfile.A2DP);
    }

    public void enforceRate(int rate, MediaController mc) {
        mRate = rate;
        mMc = mc;
        pressButton(KeyEvent.KEYCODE_MEDIA_STOP);
        mAdp.getProfileProxy(mContext, this, BluetoothProfile.A2DP);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
        Log.e("Main", "onConnected");
        BluetoothA2dp a2dp = (BluetoothA2dp) bluetoothProfile;

        List<BluetoothDevice> devs = a2dp.getConnectedDevices();
        if (devs.isEmpty()) {
            Log.e("Main", "No device found to set rate");
        } else {
            BluetoothDevice dev = devs.get(0);
            if (mActivity != null) {
                CompanionDeviceManager deviceManager = (CompanionDeviceManager) mContext.getSystemService(Context.COMPANION_DEVICE_SERVICE);
                if (!deviceManager.getAssociations().contains(dev.getAddress())) {
                    final AssociationRequest request = new AssociationRequest.Builder()
                            .addDeviceFilter(new BluetoothDeviceFilter.Builder()
                                    .setAddress(dev.getAddress())
                                    .build())
                            .setSingleDevice(true)
                            .build();

                    deviceManager.associate(request, new CompanionDeviceManager.Callback() {
                        public void onDeviceFound(IntentSender intentSender) {
                            Log.e("Main", "onDeviceFound");
                            try {
                                mActivity.startIntentSenderForResult(intentSender,
                                        42, null, 0, 0, 0, null);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e("Main", "Error: " + String.valueOf(e));
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            Log.e("Main", "Failed to associate device | Error: " + error);
                        }
                    }, null);
                }
            } else {
                String state = stateToString(a2dp.getConnectionState(dev));
                Log.e("Main", "Device: " + dev.getName()
                        + " | Connection: " + state
                        + " | Playing: " + a2dp.isA2dpPlaying(dev));

                BluetoothCodecConfig oldConfig = getConfig(a2dp);
                Log.e("Main", "OldConfig: " + oldConfig);

                BluetoothCodecConfig setConfig = getBluetoothCodecConfig();
                Log.e("Main", "SetConfig: " + setConfig);

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

                Log.e("Main", String.join("\n", txt));


            }
        }

        mAdp.closeProfileProxy(BluetoothProfile.A2DP, bluetoothProfile);
    }

    private BluetoothCodecConfig getBluetoothCodecConfig() {
        int rate = BluetoothCodecConfig.SAMPLE_RATE_48000;
        switch (mRate) {
            case 44100:
                rate = BluetoothCodecConfig.SAMPLE_RATE_44100;
                break;
            case 48000:
                rate = BluetoothCodecConfig.SAMPLE_RATE_48000;
                break;
            case 88200:
                rate = BluetoothCodecConfig.SAMPLE_RATE_88200;
                break;
            case 96000:
                rate = BluetoothCodecConfig.SAMPLE_RATE_96000;
                break;
        }

        return new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                .setSampleRate(rate)
                .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24)
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                .setCodecSpecific1(1000) // 1000 = Quality, 1003 = Best Effort
                .setCodecSpecific2(0) // 0
                .setCodecSpecific3(0) // 0
                .setCodecSpecific4(0) // 0
                .build();
    }

    @Override
    public void onServiceDisconnected(int i){
        Log.e("Main","onDisconnected");
        Handler handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
        handler.postDelayed(() -> pressButton(KeyEvent.KEYCODE_MEDIA_PLAY), PLAY_DELAY);
    }

    private String format(BluetoothCodecConfig config) {
        String[] split = config.toString().replace("{", "").replace("}", "").split(",");
        return String.join("\n", Stream.of(split).map(s -> String.join(" = ", s.split(":"))).toArray(String[]::new));
    }

    private BluetoothCodecConfig getConfig(BluetoothA2dp a2dp) {
        BluetoothDevice dev = a2dp.getConnectedDevices().get(0);
        return a2dp.getCodecStatus(dev).getCodecConfig();
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

    private void pressButton(int keyCode) {
        if (mMc != null) {
            mMc.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            mMc.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }
    }
}
