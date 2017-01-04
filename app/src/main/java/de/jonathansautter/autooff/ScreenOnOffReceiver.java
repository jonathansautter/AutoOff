package de.jonathansautter.autooff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class ScreenOnOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        SharedPreferences settingsprefs = context.getSharedPreferences("settings", 0);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

            Log.d("joo", "screen off!");

            if (!settingsprefs.getBoolean("inactivityServiceRunning", false)) {

                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
                final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Calendar cal = Calendar.getInstance();
                Date date = cal.getTime();
                long t = date.getTime();
                long delay = settingsprefs.getLong("inactivityShutdownDelay", 60000);
                Date addedMinutes = new Date(t + delay);
                cal.setTime(addedMinutes);

                settingsprefs.edit().putLong("inactivityShutdownTime", cal.getTimeInMillis()).apply();
                settingsprefs.edit().putBoolean("inactivityServiceRunning", true).apply();
                if (settingsprefs.getBoolean("inactivityServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("inactivityServiceActivated", false).apply();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                }

                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.putExtra("mode", "inactivity");
                context.startService(serviceIntent);
            }
        }
    }
}
