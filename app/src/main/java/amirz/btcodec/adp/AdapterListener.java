package amirz.btcodec.adp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.stream.Stream;

public abstract class AdapterListener implements BluetoothProfile.ServiceListener {
    private static final String TAG = "AdapterListener";

    protected final Context mContext;
    private final BluetoothAdapter mAdp;

    public AdapterListener(Context context) {
        mContext = context;
        mAdp = context.getSystemService(BluetoothManager.class).getAdapter();
    }

    public boolean connectAsync() {
        if (mAdp.getState() != BluetoothAdapter.STATE_ON) {
            return false;
        }
        return mAdp.getProfileProxy(mContext, this, BluetoothProfile.A2DP);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        Log.e(TAG, "onServiceConnected");
        BluetoothA2dp a2dp = (BluetoothA2dp) proxy;

        List<BluetoothDevice> devs = a2dp.getConnectedDevices();
        if (devs.isEmpty()) {
            Log.e(TAG, "No device connected");
            onConnected(a2dp, null);
        } else {
            if (devs.size() > 1) {
                Log.e(TAG, "Ambiguity between multiple connected bluetooth devices");
            }
            onConnected(a2dp, devs.get(0));
        }

        mAdp.closeProfileProxy(BluetoothProfile.A2DP, proxy);
    }

    protected abstract void onConnected(BluetoothA2dp a2dp, BluetoothDevice dev);

    @Override
    public void onServiceDisconnected(int profile) {
        Log.e(TAG,"onServiceDisconnected");
        onDisconnected();
    }

    protected void onDisconnected() {
    }

    protected static String format(BluetoothCodecConfig config) {
        String[] split = config.toString().replace("{", "").replace("}", "").split(",");
        return String.join("\n", Stream.of(split).map(s -> String.join(" = ", s.split(":")).replace("(", " (")).toArray(String[]::new));
    }

    protected static String stateToString(int state) {
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

    @SuppressLint("NewApi")
    protected static BluetoothCodecConfig getConfig(BluetoothA2dp a2dp, BluetoothDevice dev) {
        return HiddenApi.getCodecStatus(a2dp, dev).getCodecConfig();
    }
}
