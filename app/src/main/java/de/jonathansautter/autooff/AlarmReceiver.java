package de.jonathansautter.autooff;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;

import java.util.Calendar;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    private int seconds = 10;
    private Context ct;
    private Handler handler = new Handler();
    private String mode;
    private int shutdownmode = 1;

    @Override
    public void onReceive(final Context context, Intent intent) {

        //Log.d("AutoOff", "alarm received");

        ct = context;

        SharedPreferences settingsprefs = ct.getSharedPreferences("settings", 0);

        if (settingsprefs.getBoolean("minuteServiceRunning", false)) {
            mode = "minute";
        } else if (settingsprefs.getBoolean("timeServiceRunning", false)) {
            mode = "time";
        } else if (settingsprefs.getBoolean("bootServiceRunning", false)) {
            mode = "boot";
        } else if (settingsprefs.getBoolean("inactivityServiceRunning", false)) {
            mode = "inactivity";
        }

        if (settingsprefs.getBoolean("sysOverlay", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(ct.getApplicationContext())) {
                    shutdowncountdown();
                } else {
                    // no sys overlay dialog
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            shutdown();
                        }
                    }, 10000);
                }
            } else {
                shutdowncountdown();
            }
        } else {
            // no sys overlay dialog
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    shutdown();
                }
            }, 10000);
        }
    }

    private void shutdowncountdown() {
        final SharedPreferences settingsprefs = ct.getSharedPreferences("settings", 0);
        final WindowManager manager = (WindowManager) ct.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.alpha = 1.0f;
        layoutParams.dimAmount = 0.5f;
        layoutParams.packageName = ct.getPackageName();
        layoutParams.buttonBrightness = 1f;
        layoutParams.windowAnimations = android.R.style.Animation_Dialog;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        final View view = View.inflate(ct.getApplicationContext(), R.layout.system_alert, null);
        final TextView secondstv = (TextView) view.findViewById(R.id.seconds);

        final long[] lastShutdownTime = {0};
        switch (mode) {
            case "boot":
                lastShutdownTime[0] = settingsprefs.getLong("bootShutdownTime", 0);
                break;
            case "minute":
                lastShutdownTime[0] = settingsprefs.getLong("minuteShutdownTime", 0);
                break;
            case "time":
                lastShutdownTime[0] = settingsprefs.getLong("timeShutdownTime", 0);
                break;
            case "inactivity":
                lastShutdownTime[0] = settingsprefs.getLong("inactivityShutdownTime", 0);
                break;
        }
        final Runnable mUpdateTimeTask = new Runnable() {

            public void run() {
                if (seconds >= 0) {
                    long shutdownTime = 0;
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
                        case "inactivity":
                            shutdownTime = settingsprefs.getLong("inactivityShutdownTime", 0);
                            break;
                    }
                    if (shutdownTime == lastShutdownTime[0]) {
                        lastShutdownTime[0] = shutdownTime;
                        secondstv.setText(String.valueOf(seconds));
                        seconds--;
                        handler.postDelayed(this, 1000);
                    } else {
                        manager.removeView(view);
                    }
                } else {
                    shutdown();
                }
            }
        };

        handler.post(mUpdateTimeTask);

        FloatingActionButton cancel = (FloatingActionButton) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                manager.removeView(view);
                handler.removeCallbacks(mUpdateTimeTask);
                cancel_shutdown();
            }
        });

        FloatingActionButton extend = (FloatingActionButton) view.findViewById(R.id.extend);
        extend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                manager.removeView(view);
                handler.removeCallbacks(mUpdateTimeTask);
                extend_shutdown();
            }
        });
        manager.addView(view, layoutParams);
    }

    private void cancel_shutdown() {
        ct.stopService(new Intent(ct, NotificationService.class));

        NotificationManager nm = (NotificationManager) ct.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(1);

        SharedPreferences settingsprefs = ct.getSharedPreferences("settings", 0);
        switch (mode) {
            case "boot":
                settingsprefs.edit().putBoolean("bootServiceRunning", false).apply();
                if (settingsprefs.getBoolean("bootServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("bootServiceActivated", false).apply();
                }
                if (settingsprefs.getBoolean("timeServiceActivated", false)) {
                    // restart time mode after boot mode
                    Intent intent = new Intent(ct, AlarmReceiver.class);
                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(ct, 0, intent, 0);
                    final AlarmManager alarmManager = (AlarmManager) ct.getSystemService(Context.ALARM_SERVICE);
                    settingsprefs.edit().putBoolean("timeServiceRunning", true).apply();
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(settingsprefs.getLong("timeShutdownTime", 0));

                    if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                        cal.add(Calendar.DATE, 1);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    }

                    Intent serviceIntent = new Intent(ct, NotificationService.class);
                    serviceIntent.putExtra("mode", "time");
                    ct.startService(serviceIntent);
                }
                break;
            case "minute":
                settingsprefs.edit().putBoolean("minuteServiceRunning", false).apply();
                break;
            case "time":
                if (settingsprefs.getBoolean("timeServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();
                    settingsprefs.edit().putBoolean("timeServiceActivated", false).apply();
                } else {
                    // schedule next alarm
                    long shutdownTime = settingsprefs.getLong("timeShutdownTime", 0);
                    Calendar cal = Calendar.getInstance();
                    Date addedMinutes = new Date(shutdownTime);

                    cal.setTime(addedMinutes);
                    cal.set(Calendar.SECOND, 0);

                    AlarmManager alarmManager = (AlarmManager) ct.getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(ct, AlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(ct, 0, intent, 0);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    }

                    Intent serviceIntent = new Intent(ct, NotificationService.class);
                    settingsprefs.edit().putBoolean("timeServiceRunning", true).apply();
                    settingsprefs.edit().putBoolean("timeServiceActivated", true).apply();
                    serviceIntent.putExtra("mode", "time");

                    ct.startService(serviceIntent);
                }
                break;
            case "inactivity":
                settingsprefs.edit().putBoolean("inactivityServiceRunning", false).apply();
                if (settingsprefs.getBoolean("inactivityServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("inactivityServiceActivated", false).apply();
                }
                break;
        }
    }

    private void shutdown() {
        SharedPreferences settingsprefs = ct.getSharedPreferences("settings", 0);
        switch (mode) {
            case "boot":
                settingsprefs.edit().putBoolean("bootServiceRunning", false).commit();
                if (settingsprefs.getBoolean("bootServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("bootServiceActivated", false).commit();
                }
                break;
            case "minute":
                settingsprefs.edit().putBoolean("minuteServiceRunning", false).commit();
                break;
            case "time":
                settingsprefs.edit().putBoolean("timeServiceRunning", false).commit();
                if (settingsprefs.getBoolean("timeServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("timeServiceActivated", false).commit();
                }
                break;
            case "inactivity":
                settingsprefs.edit().putBoolean("inactivityServiceRunning", false).commit();
                if (settingsprefs.getBoolean("inactivityServiceOnce", true)) {
                    settingsprefs.edit().putBoolean("inactivityServiceActivated", false).commit();
                }
                break;
        }

        //int shutdownmode = settingsprefs.getInt("shutdownmode", 1);

        String[] shutdownCommand = new String[]{"/system/xbin/su", "-c", "reboot -p"};

        if (shutdownmode == 2) {
            shutdownCommand = new String[]{"/system/bin/su", "-c", "reboot -p"};
        } else if (shutdownmode == 3) {
            shutdownCommand = new String[]{"su", "-c", "reboot -p"};
            //shutdownCommand = new String[]{"sud", "-c", "reboot -p"}; // forced wrong command!
        }

        try {
            Process proc = Runtime.getRuntime().exec(shutdownCommand);
            proc.waitFor();
        } catch (Exception ex) {
            if (shutdownmode < 3) {
                shutdownmode++;
                shutdown();
            } else {
                cancel_shutdown();

                // open app with message dialog
                Intent mStartActivity = new Intent(ct, MainActivity.class);
                mStartActivity.putExtra("shutdownerror", true);
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(ct, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) ct.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, mPendingIntent); // takes a bit long!
                System.exit(0);
            }
            //ex.printStackTrace();
        }
    }

    private void extend_shutdown() {

        SharedPreferences settingsprefs = ct.getSharedPreferences("settings", 0);

        int extensiontime = settingsprefs.getInt("extensiontime", 10);
        long shutdownTime = 0;
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
            case "inactivity":
                shutdownTime = settingsprefs.getLong("inactivityShutdownTime", 0);
                break;
        }
        long newShutdownTime = shutdownTime + (extensiontime * 60000);
        switch (mode) {
            case "boot":
                settingsprefs.edit().putLong("bootShutdownTime", newShutdownTime).apply();
                settingsprefs.edit().putInt("lastBootShutdownDelay", settingsprefs.getInt("lastBootShutdownDelay", 0) + extensiontime).apply();
                break;
            case "minute":
                settingsprefs.edit().putLong("minuteShutdownTime", newShutdownTime).apply();
                settingsprefs.edit().putInt("lastMinuteShutdownDelay", settingsprefs.getInt("lastMinuteShutdownDelay", 0) + extensiontime).apply();
                break;
            case "time":
                settingsprefs.edit().putLong("timeShutdownTime", newShutdownTime).apply();
                break;
            case "inactivity":
                settingsprefs.edit().putLong("inactivityShutdownTime", newShutdownTime).apply();
                settingsprefs.edit().putInt("lastInactivityShutdownDelay", settingsprefs.getInt("lastInactivityShutdownDelay", 0) + extensiontime).apply();
                break;
        }

        Calendar cal = Calendar.getInstance();
        Date addedMinutes = new Date(newShutdownTime);

        cal.setTime(addedMinutes);

        AlarmManager alarmManager = (AlarmManager) ct.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ct, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ct, 0, intent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }

        Intent serviceIntent = new Intent(ct, NotificationService.class);
        switch (mode) {
            case "minute":
                settingsprefs.edit().putBoolean("minuteServiceRunning", true).apply();
                settingsprefs.edit().putBoolean("minuteServiceActivated", true).apply();
                serviceIntent.putExtra("mode", "minute");
                break;
            case "boot":
                settingsprefs.edit().putBoolean("bootServiceRunning", true).apply();
                serviceIntent.putExtra("mode", "boot");
                break;
            case "time":
                settingsprefs.edit().putBoolean("timeServiceRunning", true).apply();
                settingsprefs.edit().putBoolean("timeServiceActivated", true).apply();
                serviceIntent.putExtra("mode", "time");
                break;
            case "inactivity":
                settingsprefs.edit().putBoolean("inactivityServiceRunning", true).apply();
                serviceIntent.putExtra("mode", "inactivity");
                break;
        }
        ct.startService(serviceIntent);
    }
}