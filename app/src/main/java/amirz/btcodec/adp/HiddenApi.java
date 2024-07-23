package amirz.btcodec.adp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;

public class HiddenApi {
    public static BluetoothCodecStatus getCodecStatus(BluetoothA2dp a2dp, BluetoothDevice dev) {
        return a2dp.getCodecStatus(dev);
    }

    public static void setCodecConfigPreference(BluetoothA2dp a2dp, BluetoothDevice dev,
                                                BluetoothCodecConfig conf) {
        a2dp.setCodecConfigPreference(dev, conf);
    }
}
