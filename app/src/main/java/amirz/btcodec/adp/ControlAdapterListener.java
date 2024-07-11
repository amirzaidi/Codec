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
    private boolean mAsyncInProgress;

    public ControlAdapterListener(Context context) {
        super(context);
    }

    public void reset() {
        mRate = -1;
    }

    // Step 1: Pause, then become the controller of the bluetooth device.
    public void setRate(int rawRate, MediaController mc) {
        int rate = sRateMap.getOrDefault(rawRate, BluetoothCodecConfig.SAMPLE_RATE_48000);
        if (mRate != rate) {
            if (mAsyncInProgress) {
                Log.d(TAG, mc.getPackageName() + " request in progress, new request ignored");
            } else {
                Log.d(TAG,  mc.getPackageName() + " starting update of sample rate to " + rawRate);
                mAsyncInProgress = true;
                mRate = rate;
                mMc = mc;
                stop();
                connectAsync();
            }
        }
    }

    // Step 2: Update the sample rate.
    @Override
    protected void onConnected(BluetoothA2dp a2dp, BluetoothDevice dev) {
        updateConfig(a2dp, dev);
    }

    @Override
    protected void onDisconnected() {
        mHandler.postDelayed(this::onAsyncComplete, PLAY_DELAY);
    }

    // Step 3: Resume, and give up control.
    private void onAsyncComplete() {
        play();
        mAsyncInProgress = false;
    }

    @SuppressLint("MissingPermission")
    private void updateConfig(BluetoothA2dp a2dp, BluetoothDevice dev) {
        if (dev == null) {
            return;
        }

        String state = stateToString(a2dp.getConnectionState(dev));
        Log.d(TAG, "Device: " + dev.getName()
                + " | Connection: " + state
                + " | Playing: " + a2dp.isA2dpPlaying(dev));

        BluetoothCodecConfig oldConfig = getConfig(a2dp, dev);
        BluetoothCodecConfig setConfig = getBluetoothCodecConfig();
        if (oldConfig.equals(setConfig)) {
            return;
        }

        Log.d(TAG, "OldConfig: " + oldConfig);
        Log.d(TAG, "SetConfig: " + setConfig);
        a2dp.setCodecConfigPreference(dev, setConfig);

        long stop = System.currentTimeMillis() + RETRY_TIME;
        while (System.currentTimeMillis() <= stop && !getConfig(a2dp, dev).equals(setConfig)) {
        }

        if (getConfig(a2dp, dev).equals(setConfig)) {
            Log.d(TAG, "Update success");
            mContext.sendBroadcast(new Intent(UPDATE_INTENT));
        } else {
            Log.d(TAG, "Update failed");
        }
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
        return new BluetoothCodecConfig.Builder()
                .setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                .setSampleRate(mRate)
                .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24)
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                .setCodecSpecific1(1000) // 1000 = Quality, 1003 = Best Effort
                .setCodecSpecific2(0) // 0
                .setCodecSpecific3(0) // 0
                .setCodecSpecific4(0) // 0
                .build();
    }
}
