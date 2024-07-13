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
import java.util.function.Supplier;

import amirz.btcodec.adp.ControlAdapterListener;

public class MediaControllerCallback extends MediaController.Callback {
    private static final String TAG = "MediaControllerCallback";

    private static final int RATE_UNKNOWN = -2;
    private static final int RATE_DEFAULT = -1;

    private final Context mContext;
    private final MediaController mMc;
    //private final ControlAdapterListener mListener;
    private final Runnable mOnChange;

    private int mRate = RATE_UNKNOWN;

    public MediaControllerCallback(Context context, MediaController mc,
                                   /* ControlAdapterListener listener, */ Runnable onChange) {
        mContext = context;
        mMc = mc;
        //mListener = listener;
        mOnChange = onChange;
    }

    @SuppressLint("Range")
    @Override
    public void onMetadataChanged(MediaMetadata metadata) {
        super.onMetadataChanged(metadata);
        CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
        CharSequence album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);

        String artistString = artist == null ? "" : artist.toString().trim();
        String titleString = title == null ? "" : title.toString().trim();
        String albumString = album == null ? "" : album.toString().trim();

        String[] mProjection = {
                MediaStore.Audio.Media._ID
        };
        int artistSplit1 = artistString.lastIndexOf(' ');
        int artistSplit2 = artistString.lastIndexOf(';');
        int artistSplit = artistSplit1 == -1 ? artistSplit2 : artistSplit1;
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
                Log.e(TAG, "onMetadataChanged no results");
                resetSampleRate();
            } else {
                List<Uri> uris = new ArrayList<>();
                while (c.moveToNext()) {
                    uris.add(ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID))
                    ));
                }

                if (uris.size() > 1) {
                    Log.e(TAG, "onMetadataChanged ambiguity " + uris.size());
                }

                MediaExtractor mex = new MediaExtractor();
                mex.setDataSource(mContext, uris.get(0), null);
                MediaFormat mf = mex.getTrackFormat(0);
                int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);

                Log.d(TAG, "onMetadataChanged " + uris.get(0) + " " + sampleRate);
                changeSampleRate(sampleRate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        mOnChange.run();
        //Log.d(TAG, "onPlaybackStateChanged " + mMc.getPackageName() + " " + isPlaying(state) + " " + mIsExclusive.get());
        //if (isPlaying(state) /* && mIsExclusive.get() */) {
        //    mListener.setRate(mRate, mMc);
        //}
    }

    public int getRate() {
        return mRate;
    }

    private void resetSampleRate() {
        changeSampleRate(RATE_DEFAULT);
    }

    private void changeSampleRate(int rate) {
        Log.d(TAG,  mMc.getPackageName() + " updating sample rate to " + rate);
        mRate = rate;
        mOnChange.run();
    }

    public boolean isPlaying() {
        return isPlaying(mMc.getPlaybackState());
    }

    public static boolean isPlaying(PlaybackState state) {
        return state != null && state.getState() == PlaybackState.STATE_PLAYING;
    }
}
