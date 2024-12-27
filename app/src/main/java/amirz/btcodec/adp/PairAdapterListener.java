package amirz.btcodec.adp;

import android.annotation.NonNull;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.IntentSender;
import android.net.MacAddress;
import android.util.Log;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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
    protected void onConnected(BluetoothA2dp a2dp, BluetoothDevice dev) {
        if (dev == null) {
            return;
        }

        Log.e(TAG, "MAC1: " + dev.getAddress());
        Log.e(TAG, "MAC2: " + MacAddress.fromString(dev.getAddress()));

        CompanionDeviceManager deviceManager = (CompanionDeviceManager)
                mActivity.getSystemService(Context.COMPANION_DEVICE_SERVICE);

        List<AssociationInfo> lai = deviceManager.getMyAssociations();
        Log.e(TAG, "LAI: " + lai.size());
        for (AssociationInfo ai : lai) {
            Log.e(TAG, "AI: " + ai);
        }

        MacAddress btMac = MacAddress.fromString(dev.getAddress());
        if (deviceManager.getMyAssociations().stream().noneMatch(v -> Objects.equals(v.getDeviceMacAddress(), btMac))) {
            final AssociationRequest request = new AssociationRequest.Builder()
                    .addDeviceFilter(new BluetoothDeviceFilter.Builder()
                            .setAddress(dev.getAddress())
                            .build())
                    .setSingleDevice(true)
                    .build();

            deviceManager.associate(request, new CompanionDeviceManager.Callback() {
                public void onAssociationPending(IntentSender intentSender) {
                    Log.e(TAG, "onAssociationPending");
                    try {
                        mActivity.startIntentSenderForResult(intentSender,
                                REQUEST_CODE, null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Error: " + String.valueOf(e));
                    }
                }

                public void onAssociationCreated(AssociationInfo associationInfo) {
                    Log.e(TAG, "onAssociationCreated");
                    //mActivity.setText(format(getConfig(a2dp, dev)));

                    List<AssociationInfo> lai = deviceManager.getMyAssociations();
                    Log.e(TAG, "LAI: " + lai.size());
                    for (AssociationInfo ai : lai) {
                        Log.e(TAG, "AI: " + ai);
                    }

                    //Log.e(TAG, "CONFIG: " + HiddenApi.getCodecStatus(a2dp, dev));
                }

                @Override
                public void onFailure(CharSequence error) {
                    Log.e(TAG, "Failed to associate device | Error: " + error);
                }
            }, null);
        } else {
            mActivity.setText("All set");
            //mActivity.setText(format(getConfig(a2dp, dev)));
        }
    }
}
