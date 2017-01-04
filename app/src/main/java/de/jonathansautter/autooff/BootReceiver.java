package de.jonathansautter.autooff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Date;

public class BootReceiver extends BroadcastReceiver {

    private Context ct;
    private SharedPreferences settingsprefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            ct = context;

            settingsprefs = context.getSharedPreferences("settings", 0);

            if (settingsprefs.getBoolean("bootServiceActivated", false)) {
                settingsprefs.edit().putBoolean("timeServiceActivated", false).apply();
                settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();
                setNotification("boot");
            } else if (settingsprefs.getBoolean("timeServiceActivated", false)) {
                setNotification("time");
            }
        }
    }

    private void setNotification(String mode) {
        Intent alarmIntent = new Intent(ct, AlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(ct, 0, alarmIntent, 0);
        final AlarmManager alarmManager = (AlarmManager) ct.getSystemService(Context.ALARM_SERVICE);

        switch (mode) {
            case "boot":
                Calendar cal = Calendar.getInstance();
                Date date = cal.getTime();
                long t = date.getTime();
                long delay = settingsprefs.getLong("bootShutdownDelay", 60000);
                Date addedMinutes = new Date(t + delay);
                cal.setTime(addedMinutes);

                settingsprefs.edit().putLong("bootShutdownTime", cal.getTimeInMillis()).apply();
                settingsprefs.edit().putBoolean("bootServiceRunning", true).apply();
                if (settingsprefs.getBoolean("bootServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("bootServiceActivated", false).apply();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                }

                Intent serviceIntent = new Intent(ct, NotificationService.class);
                serviceIntent.putExtra("mode", "boot");
                ct.startService(serviceIntent);
                break;
            case "time":
                Calendar cal2 = Calendar.getInstance();
                long timeShutdownTime = settingsprefs.getLong("timeShutdownTime", 0);
                cal2.setTimeInMillis(timeShutdownTime);
                cal2.set(Calendar.SECOND, 0);

                if (System.currentTimeMillis() >= cal2.getTimeInMillis()) {
                    cal2.add(Calendar.DATE, 1);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), pendingIntent);
                }

                settingsprefs.edit().putLong("timeShutdownTime", cal2.getTimeInMillis()).apply();
                settingsprefs.edit().putBoolean("timeServiceRunning", true).apply();
                if (settingsprefs.getBoolean("timeServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("timeServiceActivated", false).apply();
                }

                Intent serviceIntent2 = new Intent(ct, NotificationService.class);
                serviceIntent2.putExtra("mode", "time");
                ct.startService(serviceIntent2);
                break;
        }
    }
}