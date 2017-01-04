package de.jonathansautter.autooff;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.github.clans.fab.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Minute_Fragment extends android.support.v4.app.Fragment {

    private View v;
    private TextView progresstv;
    private FloatingActionButton start, extend;
    private SharedPreferences settingsprefs;
    private boolean minuteServiceRunning;
    private RelativeLayout timelayout;
    private Handler handler = new Handler();
    private CountDownTimer countDownTimer;
    private CircularSeekBar seekbar;
    private long duration;
    private long now;
    private boolean timeServiceRunning;
    private boolean bootServiceRunning;
    private Animation fab_in;
    private Animation fade_in;
    private Animation zoom_out;
    private Animation fab_in2;
    private Animation fade_out;
    private Animation zoom_in;
    private Animation fab_out;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.minute_fragment, container, false);

        return v;
    }

    private void setup() {

        settingsprefs = getActivity().getSharedPreferences("settings", 0);

        fab_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_in);
        fab_in2 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_in);
        fab_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_out);
        fade_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        zoom_out = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_out);
        fade_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        zoom_in = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_in);

        minuteServiceRunning = settingsprefs.getBoolean("minuteServiceRunning", false);
        timeServiceRunning = settingsprefs.getBoolean("timeServiceRunning", false);
        bootServiceRunning = settingsprefs.getBoolean("bootServiceRunning", false);

        seekbar = (CircularSeekBar) v.findViewById(R.id.circularSeekBar1);
        progresstv = (TextView) v.findViewById(R.id.progrsstv);
        timelayout = (RelativeLayout) v.findViewById(R.id.timelayout);

        seekbar.setProgress(0);
        seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
        seekbar.setLockEnabled(true);
        seekbar.setOnSeekBarChangeListener(new CircleSeekBarListener());

        start = (FloatingActionButton) v.findViewById(R.id.start);
        extend = (FloatingActionButton) v.findViewById(R.id.extend);


        RelativeLayout main = (RelativeLayout) v.findViewById(R.id.main);
        main.startAnimation(fade_in);

        if (timeServiceRunning || bootServiceRunning) {
            start.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));

            timelayout.startAnimation(zoom_out);
            seekbar.setVisibility(View.VISIBLE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    seekbarStartAnimation();
                }
            }, 600);
        } else {
            if (minuteServiceRunning) {
                start.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.stop, null));
                seekbar.setVisibility(View.INVISIBLE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        start.startAnimation(fab_in);
                        start.setVisibility(View.VISIBLE);
                    }
                }, 1000);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        extend.startAnimation(fab_in2);
                        extend.setVisibility(View.VISIBLE);
                    }
                }, 1500);
                countdownTime();
                if (!isMyServiceRunning(NotificationService.class)) {
                    setNotification();
                }
            } else {
                start.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
                timelayout.startAnimation(zoom_out);
                seekbar.setVisibility(View.VISIBLE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        seekbarStartAnimation();
                    }
                }, 600);
            }
        }

        extend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int extensiontime = settingsprefs.getInt("extensiontime", 10);
                long shutdownTime = settingsprefs.getLong("minuteShutdownTime", 0);
                long newShutdownTime = shutdownTime + (extensiontime * 60000);

                settingsprefs.edit().putLong("minuteShutdownTime", newShutdownTime).apply();
                settingsprefs.edit().putInt("lastMinuteShutdownDelay", settingsprefs.getInt("lastMinuteShutdownDelay", 0) + extensiontime).apply();

                Calendar cal = Calendar.getInstance();
                Date addedMinutes = new Date(newShutdownTime);
                cal.setTime(addedMinutes);
                //settingsprefs.edit().putLong("lastMinuteShutdownTime", cal.getTimeInMillis()).apply();

                Intent intent2 = new Intent(getActivity(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent2, 0);
                final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
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
            }
        });

        start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                minuteServiceRunning = settingsprefs.getBoolean("minuteServiceRunning", false);
                timeServiceRunning = settingsprefs.getBoolean("timeServiceRunning", false);
                bootServiceRunning = settingsprefs.getBoolean("bootServiceRunning", false);

                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

                if (timeServiceRunning) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(settingsprefs.getLong("timeShutdownTime", 0));

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String time = sdf.format(cal.getTime());

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(getActivity().getString(R.string.timeralreadyrunning));
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownat) + time + " " + getString(R.string.uhr) + getActivity().getString(R.string.stoptimernowandshutdownin) + progresstv.getText().toString() + getActivity().getString(R.string.minutesinstead));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            timeServiceRunning = false;

                            start.performClick();
                            dialog.dismiss();
                        }

                    });

                    builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                } else if (bootServiceRunning) {
                    int shutdowndelay = settingsprefs.getInt("lastBootShutdownDelay", 0);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.timeralreadyrunning);
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownin) + shutdowndelay + getActivity().getString(R.string.stoptimernowandshutdownin) + progresstv.getText().toString() + getActivity().getString(R.string.minutesinstead));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("bootServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            start.performClick();
                            dialog.dismiss();
                        }

                    });

                    builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    if (minuteServiceRunning) {
                        alarmManager.cancel(pendingIntent);
                        pendingIntent.cancel();

                        settingsprefs.edit().putBoolean("minuteServiceRunning", false).apply();

                        getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                        cancelTimer();
                    } else {
                        if (getRootAccess()) {
                            Calendar cal = Calendar.getInstance();
                            Date date = cal.getTime();
                            long t = date.getTime();
                            Date addedMinutes = new Date(t + (Integer.parseInt(progresstv.getText().toString()) * 60000));

                            cal.setTime(addedMinutes);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                            }

                            setNotification();

                            minuteServiceRunning = true;
                            settingsprefs.edit().putBoolean("minuteServiceRunning", minuteServiceRunning).apply();
                            settingsprefs.edit().putLong("minuteShutdownTime", cal.getTimeInMillis()).apply();
                            settingsprefs.edit().putInt("lastMinuteShutdownDelay", Integer.parseInt(progresstv.getText().toString())).apply();
                            settingsprefs.edit().putInt("lastUsedTab", 0).apply();

                            start.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.stop, null));
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    extend.startAnimation(fab_in);
                                    extend.setVisibility(View.VISIBLE);
                                }
                            }, 1500);

                            seekbar.startAnimation(fade_out);
                            seekbar.setVisibility(View.INVISIBLE);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    timelayout.startAnimation(zoom_in);
                                }
                            }, 200);

                            countdownTime();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                            builder.setTitle(R.string.nosupermission);

                            SpannableString s = new SpannableString(getString(R.string.roothint));
                            Linkify.addLinks(s, Linkify.ALL);

                            builder.setMessage(s);
                            builder.setPositiveButton(getActivity().getString(R.string.close), new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }

                            });

                            AlertDialog alert = builder.create();
                            alert.show();
                            ((TextView) alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                        }
                    }
                }
            }
        });
    }

    private void cancelTimer() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            minuteServiceRunning = false;
            countDownTimer.cancel();

            int restTime = Integer.parseInt(progresstv.getText().toString());
            seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
            if (restTime > seekbar.getMax()) {
                restTime = settingsprefs.getInt("lastMinuteShutdownDelay", seekbar.getMax() / 2);
                progresstv.setText(String.valueOf(restTime));
            }
            seekbar.setProgress(restTime);

            start.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
            extend.startAnimation(fab_out);
            extend.setVisibility(View.GONE);
            timelayout.startAnimation(zoom_out);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    seekbar.startAnimation(fade_in);
                    seekbar.setVisibility(View.VISIBLE);
                }
            }, 200);
        }
    }

    private void setNotification() {
        Intent serviceIntent = new Intent(getActivity(), NotificationService.class);
        serviceIntent.putExtra("mode", "minute");
        getActivity().startService(serviceIntent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void seekbarStartAnimation() {

        final int[] startTime = {0};
        int endTime = settingsprefs.getInt("lastMinuteShutdownDelay", 30);
        if (endTime > seekbar.getMax()) {
            endTime = seekbar.getMax() / 2;
        }
        final int finalEndTime = endTime;
        int[] speed = {0};
        if (endTime != 0) {
            speed = new int[]{1000 / endTime};
        }
        final int[] finalSpeed = speed;

        new Thread(new Runnable() {

            public void run() {
                if (startTime[0] == finalEndTime) {
                    start.post(new Runnable() {
                        @Override
                        public void run() {
                            start.startAnimation(fab_in);
                            start.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    while (startTime[0] < finalEndTime) {
                        if (finalSpeed[0] > 50) {
                            finalSpeed[0] = 50;
                        }
                        try {
                            Thread.sleep(finalSpeed[0]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        seekbar.post(new Runnable() {

                            public void run() {
                                seekbar.setProgress(startTime[0]);
                            }
                        });
                        startTime[0]++;
                        if (startTime[0] == finalEndTime) {
                            start.post(new Runnable() {
                                @Override
                                public void run() {
                                    start.startAnimation(fab_in);
                                    start.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                }
            }
        }).start();
    }

    private void countdownTime() {
        now = System.currentTimeMillis();
        long endTime = settingsprefs.getLong("minuteShutdownTime", 0);
        if (settingsprefs.getBoolean("sysOverlay", false)) {
            // make it run 10 seconds longer since the shutdown countdown dialog can extent or cancel the timer
            endTime = endTime + 12000;
        }
        duration = endTime - now;

        createCountDownTimer();
    }

    private void createCountDownTimer() {
        countDownTimer = new CountDownTimer(duration, 1000) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {
                if (settingsprefs.getBoolean("minuteServiceRunning", false)) {
                    progresstv.setText(String.valueOf(((millisUntilFinished - 12000) / 60000) + 1));

                    long endTime = settingsprefs.getLong("minuteShutdownTime", 0);
                    if (settingsprefs.getBoolean("sysOverlay", false)) {
                        endTime = endTime + 12000;
                    }
                    long newduration = endTime - now;
                    if (newduration != duration) {
                        duration = newduration;
                        countDownTimer.cancel();
                        createCountDownTimer();
                    }
                } else {
                    cancelTimer();
                }
            }

            public void onFinish() {
                minuteServiceRunning = false;
                //settingsprefs.edit().putBoolean("countdownServiceRunning", countdownServiceRunning).apply();
            }
        }.start();
    }

    private boolean getRootAccess() {
        boolean gotRoot;
        try {
            Process p = Runtime.getRuntime().exec("su");
            gotRoot = true;
        } catch (IOException e) {
            e.printStackTrace();
            gotRoot = false;
        }
        return gotRoot;
    }

    public class CircleSeekBarListener implements CircularSeekBar.OnCircularSeekBarChangeListener {

        @Override
        public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {

            progresstv.setText(String.valueOf(progress));
        }

        @Override
        public void onStopTrackingTouch(CircularSeekBar seekBar) {
            if (seekBar.getProgress() == 0) {
                start.startAnimation(fab_out);
                start.setVisibility(View.INVISIBLE);
            } else {
                if (!start.isShown()) {
                    start.startAnimation(fab_in);
                    start.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(CircularSeekBar seekBar) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        settingsprefs = getActivity().getSharedPreferences("settings", 0);
        seekbar = (CircularSeekBar) v.findViewById(R.id.circularSeekBar1);
        seekbar.setProgress(0);
        seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            setup();
        }
    }

    public void refresh() {
        if (!minuteServiceRunning) {
            settingsprefs = getActivity().getSharedPreferences("settings", 0);
            seekbar = (CircularSeekBar) v.findViewById(R.id.circularSeekBar1);
            seekbar.setProgress(0);
            seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
            Activity activity = getActivity();
            if (isAdded() && activity != null) {
                setup();
            }
        }
    }
}
