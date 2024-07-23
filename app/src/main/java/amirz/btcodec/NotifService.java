package amirz.btcodec;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import amirz.btcodec.adp.ControlAdapterListener;

public class NotifService extends NotificationListenerService
        implements MediaSessionManager.OnActiveSessionsChangedListener {

    private static final String TAG = "NotifService";

    private ControlAdapterListener mListener;
    private BluetoothConnectReceiver mConnect;
    private ComponentName mComponent;
    private MediaSessionManager mMedia;
    private String mLog;

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
                    getApplicationContext(), mc, this::onChange);
                mc.registerCallback(cb);
                mCbs.put(mc, cb);
                cb.onMetadataChanged(mc.getMetadata());
            }
        }
    }

    private void onChange() {
        try {
            List<Map.Entry<MediaController, MediaControllerCallback>> cbs =
                    mCbs.entrySet()
                            .stream()
                            .filter(cb -> cb.getValue().isPlaying())
                            .collect(Collectors.toList());

            String log = cbs.stream()
                    .map(cb -> cb.getKey().getPackageName())
                    .collect(Collectors.joining(", "));
            if (!log.equals(mLog)) {
                Log.d(TAG, "onChange playing: " + log);
                mLog = log;
            }
            if (cbs.size() == 1) {
                Map.Entry<MediaController, MediaControllerCallback> mc = cbs.get(0);
                mListener.setRate(mc.getValue().getRate(), mc.getValue().getDepth(), mc.getKey());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
