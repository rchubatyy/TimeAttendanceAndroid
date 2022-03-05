package app.olivs.OnTime.Receivers;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import app.olivs.OnTime.Utilities.DataManager;

public class RestartReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        String time = DataManager.getInstance().getNotificationTime(context);
        try {
            scheduleNotify(time, context);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void scheduleNotify(String when, Context context) throws ParseException {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CheckOutNotifier.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm");
        if (when!=null) {
            Date date = format.parse(when);
            System.out.println(date);
            assert date != null;
            am.set(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
        }
        else
            am.cancel(pendingIntent);
    }
}