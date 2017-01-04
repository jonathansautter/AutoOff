package de.jonathansautter.autooff;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationService extends Service {

    public String counter;
    private NotificationManager nm;
    private CountDownTimer countdownTimer;
    long duration;
    private long now;
    private SharedPreferences settingsprefs;
    private String mode = "";
    private long lastendTime;
    private SensorManager mSensorManager;
    private ShakeDetector mShakeDetector;
    private boolean additionalShakeDelay = false;
    private boolean shakeDetectionActive = false;
    private boolean shutdownsoundplayed;
    private boolean shutdownHapticplayed;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        settingsprefs = getSharedPreferences("settings", 0);

        now = System.currentTimeMillis();

        if (intent != null) {
            if (intent.getExtras() != null) {
                mode = (String) intent.getExtras().get("mode");
                additionalShakeDelay = intent.getBooleanExtra("additionalShakeDelay", false);
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        long endTime = 0;
        switch (mode) {
            case "boot":
                endTime = settingsprefs.getLong("bootShutdownTime", 0);
                break;
            case "minute":
                endTime = settingsprefs.getLong("minuteShutdownTime", 0);
                break;
            case "time":
                endTime = settingsprefs.getLong("timeShutdownTime", 0);
                break;
            case "inactivity":
                endTime = settingsprefs.getLong("inactivityShutdownTime", 0);
                break;
        }
        lastendTime = endTime;
        duration = endTime - now;
        Calendar calll = Calendar.getInstance();
        calll.setTimeInMillis(endTime);

        createCountDownTimer();
        return START_STICKY;
    }

    private void createCountDownTimer() {
        countdownTimer = new CountDownTimer(duration, 1000) {
            MediaPlayer mMediaPlayer = null;

            @Override
            public void onTick(long millisUntilFinished) {
                counter = String.format(Locale.getDefault(), "%d min", TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) + 1);
                if (mode.equals("time")) {
                    declareTimeNotification();
                } else {
                    declareMinuteNotification();
                }

                int headsUpTime = settingsprefs.getInt("headsUpTime", 60000);
                if (headsUpTime < duration) {
                    if (settingsprefs.getBoolean("acousticalHeadsUpActive", false) && !shutdownsoundplayed) {
                        if (mMediaPlayer == null || settingsprefs.getBoolean("shutdownSoundChanged", false)) {
                            Log.d("AutoOff", "create mediaplayer");
                            int sound = R.raw.sound1;
                            if (settingsprefs.getInt("shutdownSound", 1) == 2) {
                                sound = R.raw.sound2;
                            } else if (settingsprefs.getInt("shutdownSound", 1) == 3) {
                                sound = R.raw.sound3;
                            }
                            settingsprefs.edit().putBoolean("shutdownSoundChanged", false).apply();
                            mMediaPlayer = MediaPlayer.create(NotificationService.this, sound);
                        }
                        if (mMediaPlayer != null) {
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setLooping(false);
                            if (!shutdownsoundplayed && (millisUntilFinished <= headsUpTime)) {
                                mMediaPlayer.start();
                                shutdownsoundplayed = true;
                            }
                        }
                    }

                    if (settingsprefs.getBoolean("hapticHeadsUpActive", false) && !shutdownHapticplayed) {
                        if (millisUntilFinished <= headsUpTime) {
                            Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (mVibrator.hasVibrator()) {
                                long[] pattern = {0, 300, 100, 500};
                                mVibrator.vibrate(pattern, -1);
                            }
                            shutdownHapticplayed = true;
                        }
                    }
                }

                long endTime = 0;
                switch (mode) {
                    case "boot":
                        endTime = settingsprefs.getLong("bootShutdownTime", 0);
                        break;
                    case "minute":
                        endTime = settingsprefs.getLong("minuteShutdownTime", 0);
                        break;
                    case "time":
                        endTime = settingsprefs.getLong("timeShutdownTime", 0);
                        break;
                    case "inactivity":
                        endTime = settingsprefs.getLong("inactivityShutdownTime", 0);
                        break;
                }
                if (lastendTime != endTime) {
                    now = System.currentTimeMillis();
                }
                long newduration = endTime - now;
                lastendTime = endTime;
                if (newduration != duration) {
                    countdownTimer.cancel();
                    duration = newduration;
                    createCountDownTimer();
                }
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }

    private void handleShakeEvent() {
        Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator.hasVibrator()) {
            mVibrator.vibrate(800);
        }
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
            case "inactivity":
                shutdownTime = settingsprefs.getLong("inactivityShutdownTime", 0);
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
            case "inactivity":
                settingsprefs.edit().putLong("inactivityShutdownTime", newShutdownTime).apply();
                break;
        }

        switch (mode) {
            case "minute": {
                Calendar cal = Calendar.getInstance();
                Date addedMinutes = new Date(newShutdownTime);
                cal.setTime(addedMinutes);
                settingsprefs.edit().putLong("minuteShutdownTime", cal.getTimeInMillis()).apply();

                Intent intent2 = new Intent(getApplicationContext(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent2, 0);
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
                settingsprefs.edit().putLong("timeShutdownTime", cal.getTimeInMillis()).apply();

                Intent intent2 = new Intent(getApplicationContext(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent2, 0);
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
            case "boot": {
                Calendar cal = Calendar.getInstance();
                Date addedMinutes = new Date(newShutdownTime);
                cal.setTime(addedMinutes);
                settingsprefs.edit().putLong("bootShutdownTime", cal.getTimeInMillis()).apply();

                Intent intent2 = new Intent(getApplicationContext(), AlarmReceiver.class);
                intent2.putExtra("bootAlarm", true);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent2, 0);
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
            case "inactivity": {
                Calendar cal = Calendar.getInstance();
                Date addedMinutes = new Date(newShutdownTime);
                cal.setTime(addedMinutes);
                settingsprefs.edit().putLong("inactivityShutdownTime", cal.getTimeInMillis()).apply();

                Intent intent2 = new Intent(getApplicationContext(), AlarmReceiver.class);
                intent2.putExtra("inactivityAlarm", true);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent2, 0);
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

    private void startShakeDetection() {
        shakeDetectionActive = true;
        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

        final boolean finalAdditionalShakeDelay = additionalShakeDelay;
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake() {
                if (finalAdditionalShakeDelay) {
                    if (System.currentTimeMillis() > now + 3000) {
                        handleShakeEvent();
                    }
                } else {
                    handleShakeEvent();
                }
            }
        });
    }

    private void stopShakeDetection() {
        shakeDetectionActive = false;
        // unregister shake detector
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
    }

    public void declareMinuteNotification() {

        if (settingsprefs.getBoolean("shakeToExtend", false)) {
            if (!shakeDetectionActive) {
                startShakeDetection();
            }
        } else {
            if (shakeDetectionActive) {
                stopShakeDetection();
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent cancelIntent = new Intent(this, NotificationClickReceiver.class);
        cancelIntent.putExtra("action", "cancel");
        switch (mode) {
            case "boot":
                cancelIntent.putExtra("mode", "boot");
                break;
            case "minute":
                cancelIntent.putExtra("mode", "minute");
                break;
            case "inactivity":
                cancelIntent.putExtra("mode", "inactivity");
                break;
        }
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent extendIntent = new Intent(this, NotificationClickReceiver.class);
        extendIntent.putExtra("action", "extend");
        switch (mode) {
            case "boot":
                extendIntent.putExtra("mode", "boot");
                break;
            case "minute":
                extendIntent.putExtra("mode", "minute");
                break;
            case "inactivity":
                extendIntent.putExtra("mode", "inactivity");
                break;
        }
        PendingIntent pendingIntentExtend = PendingIntent.getBroadcast(this, 2, extendIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notification = builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification_icon).setTicker(getString(R.string.app_name)).setWhen(System.currentTimeMillis())
                .setAutoCancel(false).setContentTitle(getString(R.string.app_name)).setOngoing(true)
                .setContentText(getString(R.string.shutdownin) + counter)
                .addAction(R.drawable.stop, getString(R.string.stop), pendingIntentCancel)
                .addAction(R.drawable.addtime, getString(R.string.extend), pendingIntentExtend)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        nm.notify(1, notification);
    }

    public void declareTimeNotification() {
        if (settingsprefs.getBoolean("shakeToExtend", false)) {
            if (!shakeDetectionActive) {
                startShakeDetection();
            }
        } else {
            if (shakeDetectionActive) {
                stopShakeDetection();
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent cancelIntent = new Intent(this, NotificationClickReceiver.class);
        cancelIntent.putExtra("action", "cancel");
        cancelIntent.putExtra("mode", "time");
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent extendIntent = new Intent(this, NotificationClickReceiver.class);
        extendIntent.putExtra("action", "extend");
        extendIntent.putExtra("mode", "time");
        PendingIntent pendingIntentExtend = PendingIntent.getBroadcast(this, 2, extendIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar now = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(settingsprefs.getLong("timeShutdownTime", 0));
        cal.set(Calendar.SECOND, 0);

        String tomorrow = "";
        if ((cal.get(Calendar.DATE) - now.get(Calendar.DATE)) == 1) {
            tomorrow = getString(R.string.tomorrow);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(cal.getTime());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notification = builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification_icon).setTicker(getString(R.string.app_name)).setWhen(System.currentTimeMillis())
                .setAutoCancel(false).setContentTitle(getString(R.string.app_name)).setOngoing(true)
                .setContentText(getString(R.string.shutdown) + " " + tomorrow + getString(R.string.at) + " " + time + " " + getString(R.string.uhr))
                .addAction(R.drawable.stop, getString(R.string.stop), pendingIntentCancel)
                .addAction(R.drawable.addtime, getString(R.string.extend), pendingIntentExtend)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        nm.notify(1, notification);
    }

    public void onDestroy() {
        super.onDestroy();
        countdownTimer.cancel();
        nm.cancel(1);
        stopShakeDetection();
    }
}