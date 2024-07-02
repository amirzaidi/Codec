package amirz.btcodec;

import android.Manifest;
import android.annotation.StringRes;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import amirz.btcodec.adp.PairAdapterListener;

@SuppressLint({"MissingPermission", "SetTextI18n"})
public class MainActivity extends Activity {

    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";

    private PairAdapterListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_long);
        mListener = new PairAdapterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    private void setText(@StringRes int resid) {
        TextView tv = findViewById(R.id.textView);
        tv.setText(resid);
    }

    public void setText(CharSequence... texts) {
        TextView tv = findViewById(R.id.textView);
        tv.setText(String.join("\n\n", texts));
    }
}
