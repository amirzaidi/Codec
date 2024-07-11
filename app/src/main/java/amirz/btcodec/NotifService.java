package amirz.btcodec;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amirz.btcodec.adp.ControlAdapterListener;

public class NotifService extends NotificationListenerService
        implements MediaSessionManager.OnActiveSessionsChangedListener {

    private static final String TAG = "Notif";

    private ControlAdapterListener mListener;
    private BluetoothConnectReceiver mConnect;
    private ComponentName mComponent;
    private MediaSessionManager mMedia;

    private final Map<MediaController, MediaControllerCallback> mCbs = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        mListener = new ControlAdapterListener(this);
        mConnect = new BluetoothConnectReceiver(this) {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive " + intent.getAction());
                mListener.reset();
            }
        };
        mComponent = new ComponentName(this, getClass());
        mMedia = getSystemService(MediaSessionManager.class);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.e(TAG, "onListenerConnected");
        mConnect.register();
        mMedia.addOnActiveSessionsChangedListener(this, mComponent);
        onActiveSessionsChanged(null);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.e(TAG, "onListenerDisconnected");
        mConnect.unregister();
        mMedia.removeOnActiveSessionsChangedListener(this);
    }

    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        if (controllers == null) {
            try {
                controllers = mMedia.getActiveSessions(mComponent);
            } catch (SecurityException e) {
                controllers = Collections.emptyList();
                e.printStackTrace();
            }
        }

        Log.d(TAG, "onActiveSessionsChanged " + controllers.size());

        // Remove MCs that have disappeared.
        Map<MediaController, MediaController.Callback> copyCbs = new HashMap<>(mCbs);
        for (Map.Entry<MediaController, MediaController.Callback> cb : copyCbs.entrySet()) {
            MediaController mc = cb.getKey();
            if (!controllers.contains(mc)) {
                mc.unregisterCallback(cb.getValue());
                mCbs.remove(mc);
            }
        }

        // Add MCs that have been created.
        for (MediaController mc : controllers) {
            if (!mCbs.containsKey(mc)) {
                MediaControllerCallback cb = new MediaControllerCallback(
                    getApplicationContext(), mc, mListener, this::hasExclusiveMC);
                mc.registerCallback(cb);
                mCbs.put(mc, cb);
            }
        }
    }

    private boolean hasExclusiveMC() {
        int playing = 0;
        try {
            for (MediaController mc : mMedia.getActiveSessions(mComponent)) {
                PlaybackState state = mc.getPlaybackState();
                if (MediaControllerCallback.isPlaying(state)) {
                    playing += 1;
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return playing == 1;
    }
}
