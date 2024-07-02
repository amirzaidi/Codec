package amirz.btcodec.adp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ControlAdapterListener extends AdapterListener {
    private static final String TAG = "ControlAdapterListener";

    public static final String UPDATE_INTENT = "amirz.btcodec.UPDATE";

    private static final int RETRY_TIME = 3000;
    private static final int PLAY_DELAY = 1250;

    private static final Map<Integer, Integer> sRateMap = new HashMap<>();

    static {
        sRateMap.put(44100, BluetoothCodecConfig.SAMPLE_RATE_44100);
        sRateMap.put(48000, BluetoothCodecConfig.SAMPLE_RATE_48000);
        sRateMap.put(88200, BluetoothCodecConfig.SAMPLE_RATE_88200);
        sRateMap.put(96000, BluetoothCodecConfig.SAMPLE_RATE_96000);
    }

    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    private int mRate = -1;
    private MediaController mMc;

    public ControlAdapterListener(Context context) {
        super(context);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onBluetoothDevice(BluetoothA2dp a2dp, BluetoothDevice dev) {
        if (dev == null) {
            play();
            return;
        }

        String state = stateToString(a2dp.getConnectionState(dev));
        Log.d(TAG, "Device: " + dev.getName()
                + " | Connection: " + state
                + " | Playing: " + a2dp.isA2dpPlaying(dev));

        BluetoothCodecConfig oldConfig = getConfig(a2dp, dev);
        BluetoothCodecConfig setConfig = getBluetoothCodecConfig();
        if (oldConfig.equals(setConfig)) {
            play();
            return;
        }

        Log.d(TAG, "OldConfig: " + oldConfig);
        Log.d(TAG, "SetConfig: " + setConfig);
        a2dp.setCodecConfigPreference(dev, setConfig);

        long st = System.currentTimeMillis();
        while (System.currentTimeMillis() <= st + RETRY_TIME && !getConfig(a2dp, dev).equals(setConfig)) {
        }

        if (getConfig(a2dp, dev).equals(setConfig)) {
            Log.d(TAG, "Update success");
            mContext.sendBroadcast(new Intent(UPDATE_INTENT));
            mHandler.postDelayed(this::play, PLAY_DELAY);
        } else {
            Log.d(TAG, "Update failed");
        }
    }

    public void setRate(int rate, MediaController mc) {
        mRate = rate;
        mMc = mc;
        stop();
        connectAsync();
    }

    private void stop() {
        pressButton(KeyEvent.KEYCODE_MEDIA_STOP);
    }

    private void play() {
        pressButton(KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    private void pressButton(int keyCode) {
        if (mMc != null) {
            mMc.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            mMc.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }
    }

    private BluetoothCodecConfig getBluetoothCodecConfig() {
        int rate = sRateMap.getOrDefault(mRate, BluetoothCodecConfig.SAMPLE_RATE_48000);
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
}
