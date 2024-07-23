package amirz.btcodec;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MediaControllerCallback extends MediaController.Callback {
    private static final String TAG = "MediaControllerCallback";

    private static final String KEY_SAMPLE_RATE = MediaFormat.KEY_SAMPLE_RATE;
    private static final String KEY_BIT_DEPTH = "bits-per-sample";

    private static final int DEFAULT = -1;

    private final Context mContext;
    private final MediaController mMc;
    private final Runnable mOnChange;

    private int mRate = DEFAULT;
    private int mDepth = DEFAULT;

    public MediaControllerCallback(Context context, MediaController mc, Runnable onChange) {
        mContext = context;
        mMc = mc;
        mOnChange = onChange;
    }

    @SuppressLint("Range")
    @Override
    public void onMetadataChanged(MediaMetadata metadata) {
        super.onMetadataChanged(metadata);
        if (metadata == null) {
            return;
        }

        CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
        CharSequence album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);

        String artistString = artist == null ? "" : artist.toString().trim();
        String titleString = title == null ? "" : title.toString().trim();
        String albumString = album == null ? "" : album.toString().trim();

        String[] mProjection = {
                MediaStore.Audio.Media._ID
        };
        int artistSplit = artistString.lastIndexOf(' ');
        if (artistSplit == -1) {
            artistSplit = artistString.lastIndexOf(';');
        }
        String[] mArgs = {
                "%" + titleString + "%",
                "%" + (artistSplit == -1 || artistSplit == artistString.length() - 1
                        ? artistString
                        : artistString.substring(artistSplit + 1)) + "%",
                "%" + albumString + "%",
        };
        Log.d(TAG, "Search for " + mArgs[0] + " | " + mArgs[1] + " | " + mArgs[2]);
        try (Cursor c = mContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mProjection,
                MediaStore.Audio.Media.TITLE + " like ?"
                        + " AND " + MediaStore.Audio.Media.ARTIST + " like ?"
                        + " AND " + MediaStore.Audio.Media.ALBUM + " like ?",
                mArgs,
                null
        )) {
            if (c == null || c.getCount() == 0) {
                Log.e(TAG, "onMetadataChanged " + mMc.getPackageName() + " no results");
                changeSampleRate(DEFAULT, DEFAULT);
            } else {
                List<Uri> uris = new ArrayList<>();
                while (c.moveToNext()) {
                    uris.add(ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID))
                    ));
                }

                if (uris.size() > 1) {
                    Log.e(TAG, "onMetadataChanged " + mMc.getPackageName() + " ambiguity " + uris.size());
                }

                MediaExtractor mex = new MediaExtractor();
                mex.setDataSource(mContext, uris.get(0), null);
                MediaFormat mf = mex.getTrackFormat(0);
                int sampleRate = mf.getInteger(KEY_SAMPLE_RATE, DEFAULT);
                int bitDepth = mf.getInteger(KEY_BIT_DEPTH, DEFAULT);

                Log.d(TAG, "onMetadataChanged " + mMc.getPackageName() + " " + uris.get(0) + " sr=" + sampleRate + " bd=" + bitDepth);
                changeSampleRate(sampleRate, bitDepth);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        mOnChange.run();
    }

    public int getRate() {
        return mRate;
    }

    public int getDepth() {
        return mDepth;
    }

    private void changeSampleRate(int rate, int depth) {
        mRate = rate;
        mDepth = depth;
        mOnChange.run();
    }

    public boolean isPlaying() {
        PlaybackState state = mMc.getPlaybackState();
        return state != null
                && state.getState() == PlaybackState.STATE_PLAYING;
    }
}
