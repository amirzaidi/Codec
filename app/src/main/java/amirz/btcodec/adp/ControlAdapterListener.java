package amirz.btcodec.adp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import amirz.btcodec.MediaControllerCallback;

@SuppressLint("InlinedApi")
public class ControlAdapterListener extends AdapterListener {
    private static final String TAG = "ControlAdapterListener";

    public static final String UPDATE_INTENT = "amirz.btcodec.UPDATE";

    private static final int UPDATE_DELAY = 500;
    private static final int REWIND_THRESHOLD = 5000;
    private static final int DELAY_RELEASE = 2000;

    private static final Map<Integer, Integer> RATE_MAP = new HashMap<>();
    private static final Map<Integer, Integer> DEPTH_MAP = new HashMap<>();

    static {
        RATE_MAP.put(44100, BluetoothCodecConfig.SAMPLE_RATE_44100);
        RATE_MAP.put(48000, BluetoothCodecConfig.SAMPLE_RATE_48000);
        RATE_MAP.put(88200, BluetoothCodecConfig.SAMPLE_RATE_88200);
        RATE_MAP.put(96000, BluetoothCodecConfig.SAMPLE_RATE_96000);

        DEPTH_MAP.put(16, BluetoothCodecConfig.BITS_PER_SAMPLE_16);
        DEPTH_MAP.put(24, BluetoothCodecConfig.BITS_PER_SAMPLE_24);
        DEPTH_MAP.put(32, BluetoothCodecConfig.BITS_PER_SAMPLE_32);
    }

    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    private BluetoothA2dp mA2dp;
    private BluetoothDevice mDev;

    private int mRate;
    private int mDepth;
    private MediaController mMc;
    private boolean mAsyncInProgress;

    public ControlAdapterListener(Context context) {
        super(context);
    }

    public void reset() {
        mRate = 0;
        mDepth = 0;
    }

    /** @noinspection DataFlowIssue*/
    // Step 1: Pause, then become the controller of the bluetooth device.
    public void setRate(int rawRate, int rawDepth, MediaController mc) {
        int rate = RATE_MAP.getOrDefault(rawRate, BluetoothCodecConfig.SAMPLE_RATE_48000);
        int depth = DEPTH_MAP.getOrDefault(rawDepth, BluetoothCodecConfig.BITS_PER_SAMPLE_16);
        if (mRate != rate || mDepth != depth) {
            String pkg = mc.getPackageName();
            if (mAsyncInProgress) {
                Log.d(TAG, pkg + " request in progress, new request ignored");
            } else if (isConnected() || canConnect()) {
                Log.d(TAG,  pkg + " starting update to (sr=" + rawRate + ", bd=" + rawDepth + ")");
                mAsyncInProgress = true;
                PlaybackState ps = mc.getPlaybackState();
                stop(ps != null && ps.getPosition() < REWIND_THRESHOLD);

                mRate = rate;
                mDepth = depth;
                mMc = mc;

                mHandler.postDelayed(() -> {
                    if (isConnected()) {
                        Log.d(TAG,  pkg + " already connected, immediately updating");
                        updateConfig(mA2dp, mDev);
                    } else if (!connectAsync()) {
                        Log.d(TAG,  pkg + " connect failed");
                        mAsyncInProgress = false;
                    }
                }, UPDATE_DELAY);
            }
        }
    }

    // Step 2: Update the sample rate.
    @Override
    protected void onConnected(BluetoothA2dp a2dp, BluetoothDevice dev) {
        mA2dp = a2dp;
        mDev = dev;
        updateConfig(a2dp, dev);
    }

    // Step 3: Resume playback.
    @Override
    protected void onDisconnected() {
        mA2dp = null;
        mDev = null;
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

        //BluetoothCodecConfig oldConfig = getConfig(a2dp, dev);
        BluetoothCodecConfig setConfig = getBluetoothCodecConfig();
        //if (oldConfig.equals(setConfig)) {
        //    return;
        //}

        //Log.d(TAG, "OldConfig: " + oldConfig);

        Log.d(TAG, "SetConfig: " + setConfig);
        HiddenApi.setCodecConfigPreference(a2dp, dev, setConfig);

        //long stop = System.currentTimeMillis() + RETRY_TIME;
        //noinspection StatementWithEmptyBody
        //while (System.currentTimeMillis() <= stop && !getConfig(a2dp, dev).equals(setConfig)) {
        //}

        /*
        if (getConfig(a2dp, dev).equals(setConfig)) {
            Log.d(TAG, "Update success");
            mContext.sendBroadcast(new Intent(UPDATE_INTENT));
        } else {
            Log.d(TAG, "Update failed");
        }
        */

        Log.d(TAG, "Updated");
        //mContext.sendBroadcast(new Intent(UPDATE_INTENT));

        mHandler.postDelayed(this::onAsyncComplete, DELAY_RELEASE);
    }

    // Step 4: Give up control.
    private void onAsyncComplete() {
        play();
        mAsyncInProgress = false;
    }

    private void stop(boolean rewind) {
        pressButton(rewind
                ? KeyEvent.KEYCODE_MEDIA_STOP
                : KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    private void play() {
        if (!MediaControllerCallback.isPlaying(mMc)) {
            pressButton(KeyEvent.KEYCODE_MEDIA_PLAY);
        }
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
                .setBitsPerSample(mDepth)
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
                .setCodecSpecific1(1000) // 1000 = Quality, 1003 = Best Effort
                .setCodecSpecific2(0) // 0
                .setCodecSpecific3(0) // 0
                .setCodecSpecific4(0) // 0
                .build();
    }
}
