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
import com.github.clans.fab.FloatingActionMenu;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Boot_Fragment extends android.support.v4.app.Fragment {

    private View v;
    private TextView progresstv;
    private FloatingActionMenu fab_menu;
    private FloatingActionButton stop, extend;
    private SharedPreferences settingsprefs;
    private CountDownTimer countDownTimer;
    private boolean bootServiceRunning;
    private RelativeLayout timelayout;
    private Handler handler = new Handler();
    private CircularSeekBar seekbar;
    private long duration;
    private long now;
    private boolean bootServiceActivated;
    private Animation fade_in;
    private Animation fab_in;
    private Animation fab_in2;
    private Animation zoom_out;
    private Animation fade_out;
    private Animation zoom_in;
    private Animation fab_menu_in;
    private Animation fab_out;
    private Animation fab_menu_out;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.boot_fragment, container, false);

        /*Activity activity = getActivity();
        if (isAdded() && activity != null) {
            setup();
        }*/

        return v;
    }

    public void setup() {

        settingsprefs = getActivity().getSharedPreferences("settings", 0);

        fade_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        fab_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_in);
        fab_in2 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_in);
        zoom_out = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_out);
        fade_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        zoom_in = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_in);
        fab_menu_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_menu_in);
        fab_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_out);
        fab_menu_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_menu_out);

        seekbar = (CircularSeekBar) v.findViewById(R.id.circularSeekBar1);
        progresstv = (TextView) v.findViewById(R.id.progrsstv);
        timelayout = (RelativeLayout) v.findViewById(R.id.timelayout);

        seekbar.setProgress(0);
        seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
        seekbar.setLockEnabled(true);
        seekbar.setOnSeekBarChangeListener(new CircleSeekBarListener());

        stop = (FloatingActionButton) v.findViewById(R.id.stop);
        extend = (FloatingActionButton) v.findViewById(R.id.extend);
        fab_menu = (FloatingActionMenu) v.findViewById(R.id.fab_menu);
        fab_menu.setIconAnimated(false);
        fab_menu.setClosedOnTouchOutside(true);
        final FloatingActionButton menu_once = (FloatingActionButton) v.findViewById(R.id.menu_once);
        FloatingActionButton menu_always = (FloatingActionButton) v.findViewById(R.id.menu_always);


        RelativeLayout main = (RelativeLayout) v.findViewById(R.id.main);
        //if (main.getVisibility() == View.INVISIBLE) {
            main.startAnimation(fade_in);
        //}

        bootServiceRunning = settingsprefs.getBoolean("bootServiceRunning", false);
        bootServiceActivated = settingsprefs.getBoolean("bootServiceActivated", false);

        if (bootServiceRunning) {
            seekbar.setVisibility(View.INVISIBLE);
            if (stop.getVisibility() == View.INVISIBLE) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stop.setImageResource(R.drawable.stop);
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                    }
                }, 1000);
            }
            if (extend.getVisibility() == View.INVISIBLE) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        extend.startAnimation(fab_in2);
                        extend.setVisibility(View.VISIBLE);
                    }
                }, 1500);
            }
            if (countDownTimer == null) {
                countdownTime();
            }
            progresstv.setText(String.valueOf(settingsprefs.getInt("lastBootShutdownDelay", 30)));
            if (!isMyServiceRunning(NotificationService.class)) {
                setNotification();
            }
        } else if (bootServiceActivated) {
            seekbar.setVisibility(View.INVISIBLE);
            if (stop.getVisibility() == View.INVISIBLE) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stop.setImageResource(R.drawable.cancel);
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                    }
                }, 1000);
            }
            progresstv.setText(String.valueOf(settingsprefs.getInt("lastBootShutdownDelay", 30)));
        } else {
            timelayout.startAnimation(zoom_out);
            seekbar.setVisibility(View.VISIBLE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    seekbarStartAnimation();
                }
            }, 600);
        }

        menu_once.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (settingsprefs.getBoolean("timeServiceRunning", false) && !settingsprefs.getBoolean("timeServiceOnce", true)) { // repeating time mode on
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(settingsprefs.getLong("timeShutdownTime", 0));

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String time = sdf.format(cal.getTime());

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(getActivity().getString(R.string.timeralreadyrunning));
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownat) + time + " " + getString(R.string.uhr) + getActivity().getString(R.string.stoptimernowandstartbootmodewith));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                            final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                            final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            menu_once.performClick();
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
                } else {
                    if (getRootAccess()) {
                        bootServiceActivated = true;
                        settingsprefs.edit().putBoolean("bootServiceActivated", bootServiceActivated).apply();
                        settingsprefs.edit().putBoolean("bootServiceOnce", true).apply();
                        settingsprefs.edit().putLong("bootShutdownDelay", Integer.parseInt(progresstv.getText().toString()) * 60000).apply();
                        settingsprefs.edit().putInt("lastBootShutdownDelay", Integer.parseInt(progresstv.getText().toString())).apply();
                        settingsprefs.edit().putInt("lastUsedTab", 2).apply();

                        fab_menu.toggleMenu(false);
                        fab_menu.setVisibility(View.INVISIBLE);
                        stop.setImageResource(R.drawable.cancel);
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                        seekbar.startAnimation(fade_out);
                        seekbar.setVisibility(View.INVISIBLE);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                timelayout.startAnimation(zoom_in);
                            }
                        }, 200);
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
        });

        menu_always.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (settingsprefs.getBoolean("timeServiceRunning", false) && !settingsprefs.getBoolean("timeServiceOnce", true)) { // repeating time mode on
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(settingsprefs.getLong("timeShutdownTime", 0));

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String time = sdf.format(cal.getTime());

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(getActivity().getString(R.string.timeralreadyrunning));
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownat) + time + " " + getString(R.string.uhr) + getActivity().getString(R.string.stoptimernowandstartbootmodewith));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                            final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                            final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            menu_once.performClick();
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
                } else {
                    if (getRootAccess()) {
                        bootServiceActivated = true;
                        settingsprefs.edit().putBoolean("bootServiceActivated", bootServiceActivated).apply();
                        settingsprefs.edit().putBoolean("bootServiceOnce", false).apply();
                        settingsprefs.edit().putLong("bootShutdownDelay", Integer.parseInt(progresstv.getText().toString()) * 60000).apply();
                        settingsprefs.edit().putInt("lastBootShutdownDelay", Integer.parseInt(progresstv.getText().toString())).apply();
                        settingsprefs.edit().putInt("lastUsedTab", 2).apply();

                        fab_menu.toggleMenu(false);
                        fab_menu.setVisibility(View.INVISIBLE);
                        stop.setImageResource(R.drawable.cancel);
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                        seekbar.startAnimation(fade_out);
                        seekbar.setVisibility(View.INVISIBLE);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                timelayout.startAnimation(zoom_in);
                            }
                        }, 200);
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
        });

        extend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int extensiontime = settingsprefs.getInt("extensiontime", 10);
                long shutdownTime = settingsprefs.getLong("bootShutdownTime", 0);
                long newShutdownTime = shutdownTime + (extensiontime * 60000);
                settingsprefs.edit().putLong("bootShutdownTime", newShutdownTime).apply();
                settingsprefs.edit().putInt("lastBootShutdownDelay", settingsprefs.getInt("lastBootShutdownDelay", 0) + extensiontime).apply();

                Calendar cal = Calendar.getInstance();
                Date addedMinutes = new Date(newShutdownTime);
                cal.setTime(addedMinutes);
                settingsprefs.edit().putLong("bootShutdownTime", cal.getTimeInMillis()).apply();

                Intent intent2 = new Intent(getActivity(), AlarmReceiver.class);
                intent2.putExtra("bootAlarm", true);
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

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

                if (settingsprefs.getBoolean("bootServiceRunning", false)) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                    settingsprefs.edit().putBoolean("bootServiceRunning", false).apply();
                    getActivity().stopService(new Intent(getActivity(), NotificationService.class));
                    stopTimer();
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

                        Intent serviceIntent = new Intent(getActivity(), NotificationService.class);
                        serviceIntent.putExtra("mode", "time");
                        getActivity().startService(serviceIntent);
                    }
                } else if (settingsprefs.getBoolean("bootServiceActivated", false)) {
                    settingsprefs.edit().putBoolean("bootServiceActivated", false).apply();
                    deactivateTimer();
                }
            }
        });
    }

    private void deactivateTimer() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            bootServiceRunning = false;
            bootServiceActivated = false;
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
            int lastDelay = settingsprefs.getInt("lastBootShutdownDelay", seekbar.getMax() / 2);
            if (lastDelay > seekbar.getMax()) {
                progresstv.setText(String.valueOf(lastDelay));
            }
            seekbar.setProgress(lastDelay);

            fab_menu.startAnimation(fab_menu_in);
            fab_menu.setVisibility(View.VISIBLE);
            stop.setVisibility(View.INVISIBLE);
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

    private void stopTimer() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            bootServiceRunning = false;
            countDownTimer.cancel();
            extend.startAnimation(fab_out);
            extend.setVisibility(View.GONE);
            if (settingsprefs.getBoolean("bootServiceOnce", true)) {
                bootServiceActivated = false;

                seekbar.setMax(settingsprefs.getInt("maxminutes", 60));
                int lastDelay = settingsprefs.getInt("lastBootShutdownDelay", 30);
                if (lastDelay > seekbar.getMax()) {
                    lastDelay = seekbar.getMax() / 2;
                }
                seekbar.setProgress(lastDelay);

                fab_menu.startAnimation(fab_menu_in);
                fab_menu.setVisibility(View.VISIBLE);
                stop.setVisibility(View.INVISIBLE);
                timelayout.startAnimation(zoom_out);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        seekbar.startAnimation(fade_in);
                        seekbar.setVisibility(View.VISIBLE);
                    }
                }, 200);
            } else {
                stop.setImageResource(R.drawable.cancel);
                progresstv.setText(String.valueOf(settingsprefs.getInt("lastBootShutdownDelay", 30)));
            }
        }
    }

    private void setNotification() {
        Intent serviceIntent = new Intent(getActivity(), NotificationService.class);
        serviceIntent.putExtra("mode", "boot");
        getActivity().startService(serviceIntent);
    }

    private void countdownTime() {
        now = System.currentTimeMillis();
        long endTime = settingsprefs.getLong("bootShutdownTime", 0);
        if (settingsprefs.getBoolean("sysOverlay", false)) {
            endTime = endTime + 12000;
        }
        duration = endTime - now;

        createCountDownTimer();
    }

    private void createCountDownTimer() {
        countDownTimer = new CountDownTimer(duration, 1000) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {
                if (settingsprefs.getBoolean("bootServiceRunning", false)) {
                    progresstv.setText(String.valueOf(((millisUntilFinished - 12000) / 60000) + 1));

                    long endTime = settingsprefs.getLong("bootShutdownTime", 0);
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
                    stopTimer();
                }
            }

            public void onFinish() {
                bootServiceRunning = false;
                //settingsprefs.edit().putBoolean("bootServiceRunning", bootServiceRunning).apply();
            }
        }.start();
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
        int endTime = settingsprefs.getInt("lastBootShutdownDelay", 30);
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
                    stop.post(new Runnable() {
                        @Override
                        public void run() {
                            stop.startAnimation(fab_in);
                            stop.setVisibility(View.VISIBLE);
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
                            fab_menu.post(new Runnable() {
                                @Override
                                public void run() {
                                    fab_menu.startAnimation(fab_menu_in);
                                    fab_menu.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                }
            }
        }).start();
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
                fab_menu.startAnimation(fab_menu_out);
                fab_menu.setVisibility(View.INVISIBLE);
            } else {
                if (!fab_menu.isShown()) {
                    fab_menu.startAnimation(fab_menu_in);
                    fab_menu.setVisibility(View.VISIBLE);
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
}
