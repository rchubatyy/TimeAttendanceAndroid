package app.olivs.OnTime.Utilities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ConnectivityReceiver extends BroadcastReceiver {

    public static ConnectivityReceiverListener listener;

    public ConnectivityReceiver() {
        super();
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(final Context context, final Intent intent) {

        int status = NetworkUtil.getConnectivityStatus(context);

        if (listener != null) {
            listener.onNetworkConnectionChanged(status != 0);
        }

    }

    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }
}
