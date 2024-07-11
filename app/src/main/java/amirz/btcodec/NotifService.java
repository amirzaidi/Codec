package amirz.btcodec;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amirz.btcodec.adp.ControlAdapterListener;

public class NotifService extends NotificationListenerService
        implements MediaSessionManager.OnActiveSessionsChangedListener {

    private static final String TAG = "Notif";

    private ControlAdapterListener mListener;
    private ComponentName mComponent;
    private MediaSessionManager mMedia;

    private int mRate = -1;

    private final Map<MediaController, MediaController.Callback> mCbs = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        mListener = new ControlAdapterListener(this);
        mMedia = getSystemService(MediaSessionManager.class);
        mComponent = new ComponentName(this, getClass());
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.e(TAG, "onListenedConnected");
        mMedia.addOnActiveSessionsChangedListener(this, mComponent);
        onActiveSessionsChanged(null);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.e(TAG, "onListenedDisconnected");
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

        Log.e(TAG, "onActiveSessionsChanged " + controllers.size());
        for (Map.Entry<MediaController, MediaController.Callback> cb : mCbs.entrySet()) {
            cb.getKey().unregisterCallback(cb.getValue());
        }
        mCbs.clear();
        for (MediaController mc : controllers) {
            MediaController.Callback cb = new MediaController.Callback() {
                @SuppressLint("Range")
                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    super.onMetadataChanged(metadata);
                    CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
                    CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                    CharSequence album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);

                    String artistString = artist == null ? "" : artist.toString();
                    String titleString = title == null ? "" : title.toString();
                    String albumString = album == null ? "" : album.toString();

                    String[] mProjection = {
                        MediaStore.Audio.Media._ID
                    };
                    int artistSplit = artistString.lastIndexOf(' ');
                    String[] mArgs = {
                        titleString,
                        artistSplit == -1 || artistSplit == artistString.length() - 1
                            ? artistString
                            : ("%" + artistString.substring(artistSplit + 1)),
                        albumString,
                    };
                    try (Cursor c = getApplicationContext().getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mProjection,
                        MediaStore.Audio.Media.TITLE + " = ?"
                                + " AND " + MediaStore.Audio.Media.ARTIST + " like ?"
                                + " AND " + MediaStore.Audio.Media.ALBUM + " = ?",
                        mArgs,
                        null
                    ))
                    {
                        if (c == null || c.getCount() == 0) {
                            Log.e(TAG, "onMetadataChanged no results");
                        } else {
                            List<Uri> uris = new ArrayList<>();
                            while (c.moveToNext()) {
                                uris.add(ContentUris.withAppendedId(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID))
                                ));
                            }

                            if (uris.size() == 1) {
                                MediaExtractor mex = new MediaExtractor();
                                mex.setDataSource(getApplicationContext(), uris.get(0), null);
                                MediaFormat mf = mex.getTrackFormat(0);
                                int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                                Log.d(TAG, "onMetadataChanged " + uris.get(0) + " " + sampleRate);
                                handleSampleChange(sampleRate, mc);
                            } else {
                                Log.e(TAG, "onMetadataChanged ambiguity " + uris.size());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            mc.registerCallback(cb);
            mCbs.put(mc, cb);
        }
    }

    private void handleSampleChange(int rate, MediaController mc) {
        if (mRate != rate) {
            mRate = rate;
            mListener.setRate(rate, mc);
        }
    }
}
