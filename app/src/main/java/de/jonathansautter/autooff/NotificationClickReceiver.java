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

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences settingsprefs = context.getSharedPreferences("settings", 0);

        String action = (String) intent.getExtras().get("action");
        String mode = (String) intent.getExtras().get("mode");

        if (settingsprefs.getBoolean("pinprotection", false) && !MainActivity.main_active) {
            Intent pinIntent = new Intent(context, LockedNotificationActions.class);
            pinIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pinIntent.putExtra("pinProtectedAction", action);
            pinIntent.putExtra("pinProtectedActionMode", mode);
            context.startActivity(pinIntent);
            // collapse notification bar
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } else {
            if (action != null && mode != null) {
                if (action.equals("cancel")) {

                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    Intent intent2 = new Intent(context, AlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);

                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();

                    context.stopService(new Intent(context, NotificationService.class));

                    switch (mode) {
                        case "minute":
                            settingsprefs.edit().putBoolean("minuteServiceRunning", false).apply();
                            break;
                        case "time":
                            settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();
                            break;
                        case "boot":
                            settingsprefs.edit().putBoolean("bootServiceRunning", false).apply();
                            if (settingsprefs.getBoolean("bootServiceOnce", true)) {
                                settingsprefs.edit().putBoolean("bootServiceActivated", false).apply();
                            }
                            if (settingsprefs.getBoolean("timeServiceActivated", false)) {
                                // restart time mode after boot mode
                                settingsprefs.edit().putBoolean("timeServiceRunning", true).apply();
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(settingsprefs.getLong("timeShutdownTime", 0));
                                cal.set(Calendar.SECOND, 0);

                                if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                                    cal.add(Calendar.DATE, 1);
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                                } else {
                                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                                }

                                Intent serviceIntent = new Intent(context, NotificationService.class);
                                serviceIntent.putExtra("mode", "time");
                                context.startService(serviceIntent);
                            }
                            break;
                    }
                } else if (action.equals("extend")) {

                    int extensiontime = settingsprefs.getInt("extensiontime", 10);
                    long shutdownTime = 0;
                    long newShutdownTime;

                    switch (mode) {
                        case "boot":
                            shutdownTime = settingsprefs.getLong("bootShutdownTime", 0);
                            break;
                        case "minute":
                            shutdownTime = settingsprefs.getLong("minuteShutdownTime", 0);
                            break;
                        case "time":
                            shutdownTime = settingsprefs.getLong("timeShutdownTime", 0);
                            break;
                    }

                    newShutdownTime = shutdownTime + (extensiontime * 60000);

                    switch (mode) {
                        case "boot":
                            settingsprefs.edit().putLong("bootShutdownTime", newShutdownTime).apply();
                            break;
                        case "minute":
                            settingsprefs.edit().putLong("minuteShutdownTime", newShutdownTime).apply();
                            break;
                        case "time":
                            settingsprefs.edit().putLong("timeShutdownTime", newShutdownTime).apply();
                            break;
                    }

                    switch (mode) {
                        case "minute": {
                            Calendar cal = Calendar.getInstance();
                            Date addedMinutes = new Date(newShutdownTime);
                            cal.setTime(addedMinutes);
                            settingsprefs.edit().putLong("minuteShutdownTime", cal.getTimeInMillis()).apply();
                            settingsprefs.edit().putInt("lastMinuteShutdownDelay", settingsprefs.getInt("lastMinuteShutdownDelay", 0) + extensiontime).apply();

                            Intent intent2 = new Intent(context, AlarmReceiver.class);
                            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);
                            final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            alarmManager.cancel(pendingIntent);

                            if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                                cal.add(Calendar.DATE, 1);
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            }
                            //Log.d("AutoOff", "new shutdown time: " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
                            break;
                        }
                        case "time": {
                            Calendar cal = Calendar.getInstance();
                            Date addedMinutes = new Date(newShutdownTime);
                            cal.setTime(addedMinutes);
                            cal.set(Calendar.SECOND, 0);
                            settingsprefs.edit().putLong("timeShutdownTime", cal.getTimeInMillis()).apply();

                            Intent intent2 = new Intent(context, AlarmReceiver.class);
                            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);
                            final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            alarmManager.cancel(pendingIntent);

                            if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                                cal.add(Calendar.DATE, 1);
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            }
                            break;
                        }
                        case "boot": {
                            Calendar cal = Calendar.getInstance();
                            Date addedMinutes = new Date(newShutdownTime);
                            cal.setTime(addedMinutes);
                            settingsprefs.edit().putLong("bootShutdownTime", cal.getTimeInMillis()).apply();
                            settingsprefs.edit().putInt("lastBootShutdownDelay", settingsprefs.getInt("lastBootShutdownDelay", 0) + extensiontime).apply();

                            Intent intent2 = new Intent(context, AlarmReceiver.class);
                            intent2.putExtra("bootAlarm", true);
                            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);
                            final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            alarmManager.cancel(pendingIntent);

                            if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                                cal.add(Calendar.DATE, 1);
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            }
                            //Log.d("AutoOff", "new shutdown time: " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
                            break;
                        }
                    }
                }
            }
        }
    }
}