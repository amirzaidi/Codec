package amirz.btcodec;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public abstract class BluetoothConnectReceiver extends BroadcastReceiver {
    private final Context mContext;

    public BluetoothConnectReceiver(Context context) {
        mContext = context;
    }

    public void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    public void unregister() {
        mContext.unregisterReceiver(this);
    }
}
