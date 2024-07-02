package amirz.btcodec.adp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.IntentSender;
import android.util.Log;

import amirz.btcodec.MainActivity;

public class PairAdapterListener extends AdapterListener {
    private static final String TAG = "PairAdapterListener";

    private static final int REQUEST_CODE = 42;

    private final MainActivity mActivity;

    public PairAdapterListener(MainActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    protected void onBluetoothDevice(BluetoothA2dp a2dp, BluetoothDevice dev) {
        if (dev == null) {
            return;
        }

        CompanionDeviceManager deviceManager = (CompanionDeviceManager) mActivity.getSystemService(Context.COMPANION_DEVICE_SERVICE);
        if (!deviceManager.getAssociations().contains(dev.getAddress())) {
            final AssociationRequest request = new AssociationRequest.Builder()
                    .addDeviceFilter(new BluetoothDeviceFilter.Builder()
                            .setAddress(dev.getAddress())
                            .build())
                    .setSingleDevice(true)
                    .build();

            deviceManager.associate(request, new CompanionDeviceManager.Callback() {
                public void onDeviceFound(IntentSender intentSender) {
                    Log.e(TAG, "onDeviceFound");
                    try {
                        mActivity.startIntentSenderForResult(intentSender,
                                REQUEST_CODE, null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Error: " + String.valueOf(e));
                    }
                }

                @Override
                public void onFailure(CharSequence error) {
                    Log.e(TAG, "Failed to associate device | Error: " + error);
                }
            }, null);
        }

        mActivity.setText(format(getConfig(a2dp, dev)));
    }
}
