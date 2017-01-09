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
import android.widget.TimePicker;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Time_Fragment extends android.support.v4.app.Fragment {

    private View v;
    private SharedPreferences settingsprefs;
    private TimePicker timepicker;
    private FloatingActionMenu fab_menu;
    private FloatingActionButton stop, extend;
    private boolean timeServiceRunning;
    private Handler handler = new Handler();
    private long duration;
    private CountDownTimer countDownTimer;
    private boolean minuteServiceRunning;
    private boolean bootServiceRunning;
    private long now;
    private Calendar countdownCal;
    private RelativeLayout timelayout;
    private TextView progresstv;
    private Animation fab_in;
    private Animation fab_in2;
    private Animation fade_in;
    private Animation fab_menu_in;
    private Animation fade_out;
    private Animation zoom_in_down;
    private Animation fab_out;
    private Animation zoom_out_up;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.time_fragment, container, false);

        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            setup();
        }

        return v;
    }

    private void setup() {

        settingsprefs = getActivity().getSharedPreferences("settings", 0);

        Animation zoom_in = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_in);
        fab_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_in);
        fab_in2 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_in);
        fade_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        fab_menu_in = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_menu_in);
        fade_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        zoom_in_down = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_in_down);
        fab_out = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_out);
        zoom_out_up = AnimationUtils.loadAnimation(getActivity(), R.anim.zoom_out_up);

        timeServiceRunning = settingsprefs.getBoolean("timeServiceRunning", false);
        minuteServiceRunning = settingsprefs.getBoolean("minuteServiceRunning", false);
        bootServiceRunning = settingsprefs.getBoolean("bootServiceRunning", false);

        timelayout = (RelativeLayout) v.findViewById(R.id.timelayout);
        progresstv = (TextView) v.findViewById(R.id.progrsstv);
        timepicker = (TimePicker) v.findViewById(R.id.timePicker);
        stop = (FloatingActionButton) v.findViewById(R.id.stop);
        extend = (FloatingActionButton) v.findViewById(R.id.extend);
        fab_menu = (FloatingActionMenu) v.findViewById(R.id.fab_menu);
        fab_menu.setIconAnimated(false);
        fab_menu.setClosedOnTouchOutside(true);
        final FloatingActionButton menu_once = (FloatingActionButton) v.findViewById(R.id.menu_once);
        final FloatingActionButton menu_always = (FloatingActionButton) v.findViewById(R.id.menu_always);

        if (Locale.getDefault().getLanguage().equals("de")) {
            timepicker.setIs24HourView(true);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(settingsprefs.getLong("lastTimeShutdownTime", cal.getTimeInMillis() + (30 * 60000))));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timepicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
            timepicker.setMinute(cal.get(Calendar.MINUTE));
        } else {
            timepicker.setCurrentMinute(cal.get(Calendar.HOUR_OF_DAY));
            timepicker.setCurrentMinute(cal.get(Calendar.MINUTE));
        }

        if (!minuteServiceRunning) {
            if (timeServiceRunning) {
                timelayout.startAnimation(zoom_in);
                timelayout.setVisibility(View.VISIBLE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                    }
                }, 1000);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        extend.startAnimation(fab_in2);
                        extend.setVisibility(View.VISIBLE);
                    }
                }, 1500);
                if (!isMyServiceRunning(NotificationService.class)) {
                    setNotification();
                }
                countdownTime();
            } else {
                timepicker.startAnimation(fade_in);
                timepicker.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fab_menu.startAnimation(fab_menu_in);
                        fab_menu.setVisibility(View.VISIBLE);
                    }
                }, 1000);
            }
        } else {
            timepicker.startAnimation(fade_in);
            timepicker.setVisibility(View.VISIBLE);
            stop.setVisibility(View.GONE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fab_menu.startAnimation(fab_menu_in);
                    fab_menu.setVisibility(View.VISIBLE);
                }
            }, 1000);
        }

        extend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int extensiontime = settingsprefs.getInt("extensiontime", 10);
                long shutdownTime = settingsprefs.getLong("timeShutdownTime", 0);
                long newShutdownTime = shutdownTime + (extensiontime * 60000);

                settingsprefs.edit().putLong("timeShutdownTime", newShutdownTime).apply();

                Calendar cal = Calendar.getInstance();
                Date addedMinutes = new Date(newShutdownTime);
                cal.setTime(addedMinutes);
                cal.set(Calendar.SECOND, 0);

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
            }
        });

        menu_once.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                minuteServiceRunning = settingsprefs.getBoolean("minuteServiceRunning", false);
                timeServiceRunning = settingsprefs.getBoolean("timeServiceRunning", false);
                bootServiceRunning = settingsprefs.getBoolean("bootServiceRunning", false);

                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

                if (minuteServiceRunning) {
                    int shutdowndelay = settingsprefs.getInt("lastMinuteShutdownDelay", 0);
                    int min;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        min = timepicker.getMinute();
                    } else {
                        min = timepicker.getCurrentMinute();
                    }
                    if (min < 10) {
                        min = Integer.parseInt("0" + String.valueOf(min));
                    }
                    String timeShutdownTime;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        timeShutdownTime = "" + timepicker.getHour() + ":" + min;
                    } else {
                        timeShutdownTime = "" + timepicker.getCurrentHour() + ":" + min;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.timeralreadyrunning);
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownin) + shutdowndelay + getActivity().getString(R.string.stoptimernowandshutdownat) + timeShutdownTime + getActivity().getString(R.string.instead));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("minuteServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            menu_once.performClick();
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
                } else if (bootServiceRunning) {
                    int shutdowndelay = settingsprefs.getInt("lastBootShutdownDelay", 0);
                    int min;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        min = timepicker.getMinute();
                    } else {
                        min = timepicker.getCurrentMinute();
                    }
                    if (min < 10) {
                        min = Integer.parseInt("0" + String.valueOf(min));
                    }
                    String timeShutdownTime;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        timeShutdownTime = "" + timepicker.getHour() + ":" + min;
                    } else {
                        timeShutdownTime = "" + timepicker.getCurrentHour() + ":" + min;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.timeralreadyrunning);
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownin) + shutdowndelay + getActivity().getString(R.string.stoptimernowandshutdownat) + timeShutdownTime + getActivity().getString(R.string.instead));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("bootServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            menu_once.performClick();
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
                    if (getRootAccess()) {
                        Calendar cal = Calendar.getInstance();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cal.set(Calendar.HOUR_OF_DAY, timepicker.getHour());
                            cal.set(Calendar.MINUTE, timepicker.getMinute());
                        } else {
                            cal.set(Calendar.HOUR_OF_DAY, timepicker.getCurrentHour());
                            cal.set(Calendar.MINUTE, timepicker.getCurrentMinute());
                        }
                        cal.set(Calendar.SECOND, 0);

                        if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                            cal.add(Calendar.DATE, 1);
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                        }

                        setNotification();

                        timeServiceRunning = true;
                        settingsprefs.edit().putBoolean("timeServiceRunning", timeServiceRunning).apply();
                        settingsprefs.edit().putBoolean("timeServiceOnce", true).apply();
                        settingsprefs.edit().putLong("timeShutdownTime", cal.getTimeInMillis()).apply();
                        settingsprefs.edit().putLong("lastTimeShutdownTime", cal.getTimeInMillis()).apply();
                        settingsprefs.edit().putInt("lastUsedTab", 1).apply();

                        //stop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.stop, null));
                        fab_menu.toggleMenu(false);
                        fab_menu.setVisibility(View.INVISIBLE);
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                        timepicker.startAnimation(fade_out);
                        timepicker.setVisibility(View.INVISIBLE);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                timelayout.startAnimation(zoom_in_down);
                                timelayout.setVisibility(View.VISIBLE);
                            }
                        }, 200);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                extend.startAnimation(fab_in2);
                                extend.setVisibility(View.VISIBLE);
                            }
                        }, 1000);

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
        });

        menu_always.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minuteServiceRunning = settingsprefs.getBoolean("minuteServiceRunning", false);
                timeServiceRunning = settingsprefs.getBoolean("timeServiceRunning", false);
                bootServiceRunning = settingsprefs.getBoolean("bootServiceRunning", false);

                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

                if (minuteServiceRunning) {
                    int shutdowndelay = settingsprefs.getInt("lastMinuteShutdownDelay", 0);
                    int min;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        min = timepicker.getMinute();
                    } else {
                        min = timepicker.getCurrentMinute();
                    }
                    if (min < 10) {
                        min = Integer.parseInt("0" + String.valueOf(min));
                    }
                    String timeShutdownTime;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        timeShutdownTime = "" + timepicker.getHour() + ":" + min;
                    } else {
                        timeShutdownTime = "" + timepicker.getCurrentHour() + ":" + min;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.timeralreadyrunning);
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownin) + shutdowndelay + getActivity().getString(R.string.stoptimernowandshutdownat) + timeShutdownTime + getActivity().getString(R.string.instead));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("minuteServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            menu_always.performClick();
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
                } else if (bootServiceRunning) {
                    int shutdowndelay = settingsprefs.getInt("lastBootShutdownDelay", 0);
                    int min;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        min = timepicker.getMinute();
                    } else {
                        min = timepicker.getCurrentMinute();
                    }
                    if (min < 10) {
                        min = Integer.parseInt("0" + String.valueOf(min));
                    }
                    String timeShutdownTime;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        timeShutdownTime = "" + timepicker.getHour() + ":" + min;
                    } else {
                        timeShutdownTime = "" + timepicker.getCurrentHour() + ":" + min;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.timeralreadyrunning);
                    builder.setMessage(getActivity().getString(R.string.timeralreadysettoshutdownin) + shutdowndelay + getActivity().getString(R.string.stoptimernowandshutdownat) + timeShutdownTime + getActivity().getString(R.string.instead));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("bootServiceRunning", false).apply();

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                            menu_always.performClick();
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
                } else if (settingsprefs.getBoolean("bootServiceActivated", false)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.timeralreadyrunning);
                    builder.setMessage(getActivity().getString(R.string.bootserviceactive));

                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();

                            settingsprefs.edit().putBoolean("bootServiceActivated", false).apply();
                            PageAdapter pageAdapter = MainActivity.pageAdapter;
                            Boot_Fragment boot_fragment = (Boot_Fragment) pageAdapter.getRegisteredFragment(2);
                            boot_fragment.setup();

                            menu_always.performClick();
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
                    if (getRootAccess()) {
                        Calendar cal = Calendar.getInstance();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cal.set(Calendar.HOUR_OF_DAY, timepicker.getHour());
                            cal.set(Calendar.MINUTE, timepicker.getMinute());
                        } else {
                            cal.set(Calendar.HOUR_OF_DAY, timepicker.getCurrentHour());
                            cal.set(Calendar.MINUTE, timepicker.getCurrentMinute());
                        }
                        cal.set(Calendar.SECOND, 0);

                        if (System.currentTimeMillis() >= cal.getTimeInMillis()) {
                            cal.add(Calendar.DATE, 1);
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                        }

                        setNotification();

                        timeServiceRunning = true;
                        settingsprefs.edit().putBoolean("timeServiceRunning", timeServiceRunning).apply();
                        settingsprefs.edit().putBoolean("timeServiceActivated", true).apply();
                        settingsprefs.edit().putBoolean("timeServiceOnce", false).apply();
                        settingsprefs.edit().putLong("timeShutdownTime", cal.getTimeInMillis()).apply();
                        settingsprefs.edit().putLong("lastTimeShutdownTime", cal.getTimeInMillis()).apply();
                        settingsprefs.edit().putInt("lastUsedTab", 1).apply();

                        fab_menu.toggleMenu(false);
                        fab_menu.setVisibility(View.INVISIBLE);
                        stop.startAnimation(fab_in);
                        stop.setVisibility(View.VISIBLE);
                        timepicker.startAnimation(fade_out);
                        timepicker.setVisibility(View.INVISIBLE);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                timelayout.startAnimation(zoom_in_down);
                                timelayout.setVisibility(View.VISIBLE);
                            }
                        }, 200);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                extend.startAnimation(fab_in2);
                                extend.setVisibility(View.VISIBLE);
                            }
                        }, 1000);

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
        });

        stop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                timeServiceRunning = settingsprefs.getBoolean("timeServiceRunning", false);

                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
                final AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

                if (timeServiceRunning) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();

                    settingsprefs.edit().putBoolean("timeServiceRunning", false).apply();
                    settingsprefs.edit().putBoolean("timeServiceActivated", false).apply();

                    getActivity().stopService(new Intent(getActivity(), NotificationService.class));

                    cancelTimer();
                }
            }
        });
    }

    private void cancelTimer() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            timeServiceRunning = false;
            countDownTimer.cancel();

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(settingsprefs.getLong("timeShutdownTime", cal.getTimeInMillis() + (30 * 60000))));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timepicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
                timepicker.setMinute(cal.get(Calendar.MINUTE));
            } else {
                timepicker.setCurrentMinute(cal.get(Calendar.HOUR_OF_DAY));
                timepicker.setCurrentMinute(cal.get(Calendar.MINUTE));
            }

            extend.startAnimation(fab_out);
            extend.setVisibility(View.GONE);
            fab_menu.startAnimation(fab_menu_in);
            fab_menu.setVisibility(View.VISIBLE);
            stop.setVisibility(View.INVISIBLE);

            timelayout.startAnimation(zoom_out_up);
            timelayout.setVisibility(View.GONE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    timepicker.startAnimation(fade_in);
                    timepicker.setVisibility(View.VISIBLE);
                }
            }, 200);
        }
    }

    private void countdownTime() {
        countdownCal = Calendar.getInstance();
        now = System.currentTimeMillis();
        long endTime = settingsprefs.getLong("timeShutdownTime", 0);
        if (settingsprefs.getBoolean("sysOverlay", false)) {
            endTime = endTime + 12000;
        }
        duration = endTime - now;
        countdownCal.setTimeInMillis(endTime - 12000);

        createCountDownTimer();
    }

    private void createCountDownTimer() {
        countDownTimer = new CountDownTimer(duration, 1000) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {
                if (settingsprefs.getBoolean("timeServiceRunning", false)) {

                    long endTime = settingsprefs.getLong("timeShutdownTime", 0);
                    if (settingsprefs.getBoolean("sysOverlay", false)) {
                        endTime = endTime + 12000;
                    }
                    countdownCal.setTimeInMillis(endTime - 12000);

                    if (Locale.getDefault().getLanguage().equals("de")) {
                        progresstv.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(countdownCal.getTime())); // 24h format
                    } else {
                        progresstv.setText(new SimpleDateFormat("hh:mm", Locale.getDefault()).format(countdownCal.getTime()));
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
                timeServiceRunning = false;
            }
        }.start();
    }

    private void setNotification() {
        Intent serviceIntent = new Intent(getActivity(), NotificationService.class);
        serviceIntent.putExtra("mode", "time");
        getActivity().startService(serviceIntent);
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

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
