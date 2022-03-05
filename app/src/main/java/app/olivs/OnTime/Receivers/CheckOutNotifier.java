package app.olivs.OnTime.Receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import app.olivs.OnTime.Activities.CheckInActivity;
import app.olivs.OnTime.Activities.SplashScreenActivity;
import app.olivs.OnTime.Model.ActivityState;
import app.olivs.OnTime.R;
import app.olivs.OnTime.Utilities.DataManager;

public class CheckOutNotifier extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent openIntent = new Intent(context, SplashScreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, openIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        ActivityState state = DataManager.getInstance().getLastActivityType(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "OnTime")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(state == ActivityState.CHECKOUT ? "Time to check in" : "Time to check out")
                .setContentText(state == ActivityState.CHECKOUT ? "Please do not forget to check in.": "Please do not forget to check out.")
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(1, builder.build());
    }


}