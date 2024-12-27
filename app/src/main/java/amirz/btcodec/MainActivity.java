package amirz.btcodec;

import android.Manifest;
import android.annotation.StringRes;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import amirz.btcodec.adp.ControlAdapterListener;
import amirz.btcodec.adp.PairAdapterListener;

@SuppressLint({"MissingPermission", "SetTextI18n"})
public class MainActivity extends Activity {

    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity", "Broadcast received");
            MainActivity.this.mListener.connectAsync();
        }
    };

    private boolean mReceiverRegistered;
    private PairAdapterListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_long);
        mListener = new PairAdapterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter f = new IntentFilter();
        f.addAction(ControlAdapterListener.UPDATE_INTENT);
        registerReceiver(mReceiver, f, RECEIVER_NOT_EXPORTED);
        mReceiverRegistered = true;

        if (mListener.isConnected()) {
            return;
        }

        setText(R.string.permissions_needed);

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[] {
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                },
                0
            );
        } else {
            ComponentName cn = new ComponentName(getApplication(), NotifService.class);

            String enabledListeners = Settings.Secure.getString(
                    getContentResolver(), NOTIFICATION_ENABLED_LISTENERS);
            boolean serviceEnabled = enabledListeners != null &&
                    (enabledListeners.contains(cn.flattenToString()) ||
                            enabledListeners.contains(cn.flattenToShortString()));

            if (!serviceEnabled) {
                Bundle showFragmentArgs = new Bundle();
                showFragmentArgs.putString(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString());

                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(EXTRA_FRAGMENT_ARG_KEY, cn.flattenToString())
                        .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, showFragmentArgs);
                startActivity(intent);
            } else {
                setText(R.string.permissions_granted);
                mListener.connectAsync();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    private void setText(@StringRes int resid) {
        TextView tv = findViewById(R.id.textView);
        tv.setText(resid);
    }

    public void setText(CharSequence... texts) {
        TextView tv = findViewById(R.id.textView);
        tv.setText(String.join("\n\n", texts));
    }
}
