package de.jonathansautter.autooff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.orangegangsters.lollipin.lib.managers.AppLock;
import com.github.orangegangsters.lollipin.lib.managers.LockManager;

import java.util.Calendar;
import java.util.Date;

public class LockedNotificationActions extends AppCompatActivity {

    private SharedPreferences settingsprefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setTitle("");

        settingsprefs = getSharedPreferences("settings", 0);

        LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
        //Log.d("AutoOff", "should lock: "+lockManager.getAppLock().shouldLockSceen(LockedNotificationActions.this));

        // pin protection
        if (lockManager.getAppLock().isPasscodeSet() && lockManager.getAppLock().shouldLockSceen(LockedNotificationActions.this)) {
            Intent intent = new Intent(LockedNotificationActions.this, CustomPinActivity.class);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
        //Log.d("AutoOff", "onResume should lock: "+lockManager.getAppLock().shouldLockSceen(LockedNotificationActions.this));
        if (!lockManager.getAppLock().shouldLockSceen(LockedNotificationActions.this)) {
            // unlocked
            if (getIntent() != null) {
                if (getIntent().getExtras() != null) {
                    String action = getIntent().getStringExtra("pinProtectedAction");
                    String mode = getIntent().getStringExtra("pinProtectedActionMode");
                    if (action != null && mode != null) {
                        //Log.d("AutoOff", "do notification action");
                        if (action.equals("cancel")) {
                            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            Intent intent2 = new Intent(LockedNotificationActions.this, AlarmReceiver.class);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(LockedNotificationActions.this, 0, intent2, 0);

                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            stopService(new Intent(LockedNotificationActions.this, NotificationService.class));

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
                                    settingsprefs.edit().putLong("lastMinuteShutdownTime", cal.getTimeInMillis()).apply();

                                    Intent intent2 = new Intent(LockedNotificationActions.this, AlarmReceiver.class);
                                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(LockedNotificationActions.this, 0, intent2, 0);
                                    final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
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
                                    settingsprefs.edit().putLong("lastTimeShutdownTime", cal.getTimeInMillis()).apply();

                                    Intent intent2 = new Intent(LockedNotificationActions.this, AlarmReceiver.class);
                                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(LockedNotificationActions.this, 0, intent2, 0);
                                    final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                                    alarmManager.cancel(pendingIntent);

                                    //stopService(new Intent(LockedNotificationActions.this, NotificationService.class));

                                    if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                                        cal.add(Calendar.DATE, 1);
                                    }

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                                    } else {
                                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                                    }

                                    /*Intent serviceIntent = new Intent(LockedNotificationActions.this, NotificationService.class);
                                    serviceIntent.putExtra("mode", "time");
                                    startService(serviceIntent);*/
                                    //Log.d("AutoOff", "new shutdown time: " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
                                    break;
                                }
                                case "boot": {
                                    Calendar cal = Calendar.getInstance();
                                    Date addedMinutes = new Date(newShutdownTime);
                                    cal.setTime(addedMinutes);
                                    //settingsprefs.edit().putLong("lastBootShutdownTime", cal.getTimeInMillis()).apply();
                                    settingsprefs.edit().putLong("bootShutdownTime", cal.getTimeInMillis()).apply();

                                    Intent intent2 = new Intent(LockedNotificationActions.this, AlarmReceiver.class);
                                    intent2.putExtra("bootAlarm", true);
                                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(LockedNotificationActions.this, 0, intent2, 0);
                                    final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
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
                        LockedNotificationActions.this.finish();
                    }
                }
            }
        }
    }

    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
        LockedNotificationActions.this.finish();
    }*/
}
