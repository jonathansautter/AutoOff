package de.jonathansautter.autooff;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.orangegangsters.lollipin.lib.managers.AppLock;
import com.github.orangegangsters.lollipin.lib.managers.LockManager;

public class Settings_Fragment extends android.support.v4.app.Fragment {

    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_ENABLE = 1532;
    private View v;
    private SharedPreferences settingsprefs;
    private RelativeLayout changepin;
    private FrameLayout changepindivider;
    private FrameLayout headsUpTimeDivider;
    private FrameLayout headsUpSoundDivider;
    AlertDialog levelDialog;
    private int headsUpSoundSelection = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.settings_fragment, container, false);

        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            setup();
        }

        return v;
    }

    public void setup() {

        settingsprefs = getActivity().getSharedPreferences("settings", 0);

        RelativeLayout maxminuteslayout = (RelativeLayout) v.findViewById(R.id.maxminutes);
        RelativeLayout extensiontimelayout = (RelativeLayout) v.findViewById(R.id.extensiontime);
        RelativeLayout shaketoextendlayout = (RelativeLayout) v.findViewById(R.id.shaketoextend);
        final SwitchCompat switchshaketoextend = (SwitchCompat) v.findViewById(R.id.switchshaketoextend);
        RelativeLayout shutdowncountdownlayout = (RelativeLayout) v.findViewById(R.id.shutdowncountdown);
        final SwitchCompat switchshutdowncountdown = (SwitchCompat) v.findViewById(R.id.switchshutdowncountdown);
        RelativeLayout pinprotectionlayout = (RelativeLayout) v.findViewById(R.id.pinprotection);
        final SwitchCompat switchpinprotection = (SwitchCompat) v.findViewById(R.id.switchpinprotection);
        RelativeLayout acousticalHeadsUp = (RelativeLayout) v.findViewById(R.id.acousticalHeadsUp);
        RelativeLayout hapticHeadsUp = (RelativeLayout) v.findViewById(R.id.hapticHeadsUp);
        final SwitchCompat switchAcousticalHeadsUp = (SwitchCompat) v.findViewById(R.id.switchAcousticalHeadsUp);
        final SwitchCompat switchHapticHeadsUp = (SwitchCompat) v.findViewById(R.id.switchHapticHeadsUp);
        final RelativeLayout headsUpSound = (RelativeLayout) v.findViewById(R.id.headsUpSound);
        final RelativeLayout headsUpTime = (RelativeLayout) v.findViewById(R.id.headsUpTime);
        changepin = (RelativeLayout) v.findViewById(R.id.changepin);
        changepindivider = (FrameLayout) v.findViewById(R.id.changepindivider);
        headsUpTimeDivider = (FrameLayout) v.findViewById(R.id.headsUpTimeDivider);
        headsUpSoundDivider = (FrameLayout) v.findViewById(R.id.headsUpSoundDivider);
        RelativeLayout contactlayout = (RelativeLayout) v.findViewById(R.id.contact);

        switchshutdowncountdown.setChecked(settingsprefs.getBoolean("sysOverlay", false));
        switchpinprotection.setChecked(settingsprefs.getBoolean("pinprotection", false));
        switchAcousticalHeadsUp.setChecked(settingsprefs.getBoolean("acousticalHeadsUpActive", false));

        if (switchAcousticalHeadsUp.isChecked() || switchHapticHeadsUp.isChecked()) {
            headsUpTime.setVisibility(View.VISIBLE);
            headsUpTimeDivider.setVisibility(View.VISIBLE);
        } else {
            headsUpSound.setVisibility(View.GONE);
            headsUpSoundDivider.setVisibility(View.GONE);
            headsUpTime.setVisibility(View.GONE);
            headsUpTimeDivider.setVisibility(View.GONE);
        }

        if (switchAcousticalHeadsUp.isChecked()) {
            headsUpSound.setVisibility(View.VISIBLE);
            headsUpSoundDivider.setVisibility(View.VISIBLE);
        } else {
            headsUpSound.setVisibility(View.GONE);
            headsUpSoundDivider.setVisibility(View.GONE);
        }

        if (switchpinprotection.isChecked()) {
            changepin.setVisibility(View.VISIBLE);
            changepindivider.setVisibility(View.VISIBLE);
        } else {
            changepin.setVisibility(View.GONE);
            changepindivider.setVisibility(View.GONE);
        }

        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            shaketoextendlayout.setVisibility(View.VISIBLE);
            switchshaketoextend.setChecked(settingsprefs.getBoolean("shakeToExtend", false));
        } else {
            shaketoextendlayout.setVisibility(View.GONE);
            if (settingsprefs.getBoolean("shakeToExtend", false)) {
                settingsprefs.edit().putBoolean("shakeToExtend", false).apply();
            }
        }

        Vibrator mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator.hasVibrator()) {
            hapticHeadsUp.setVisibility(View.VISIBLE);
            switchHapticHeadsUp.setChecked(settingsprefs.getBoolean("hapticHeadsUpActive", false));
        } else {
            hapticHeadsUp.setVisibility(View.GONE);
            if (settingsprefs.getBoolean("hapticHeadsUpActive", false)) {
                settingsprefs.edit().putBoolean("hapticHeadsUpActive", false).apply();
            }
        }

        maxminuteslayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final NumberPicker npView = (NumberPicker) inflater.inflate(R.layout.number_picker_dialog_layout, null);
                npView.setMinValue(1);
                npView.setMaxValue(10000);
                npView.setValue(settingsprefs.getInt("maxminutes", 60));
                new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getActivity().getString(R.string.maxminutes))
                        .setView(npView)
                        .setPositiveButton(getActivity().getString(R.string.save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        npView.clearFocus();
                                        settingsprefs.edit().putInt("maxminutes", npView.getValue()).apply();
                                        //initiate();
                                        ((MainActivity) getActivity()).refreshTabs();
                                    }
                                })
                        .setNegativeButton(getActivity().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                    }
                                })
                        .create().show();
            }
        });

        extensiontimelayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final NumberPicker npView = (NumberPicker) inflater.inflate(R.layout.number_picker_dialog_layout, null);
                npView.setMinValue(1);
                npView.setMaxValue(10000);
                npView.setValue(settingsprefs.getInt("extensiontime", 10));
                new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(R.string.extensiontime)
                        .setView(npView)
                        .setPositiveButton(getActivity().getString(R.string.save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        npView.clearFocus();
                                        settingsprefs.edit().putInt("extensiontime", npView.getValue()).apply();
                                        //initiate();
                                        ((MainActivity) getActivity()).refreshTabs();
                                    }
                                })
                        .setNegativeButton(getActivity().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                    }
                                })
                        .create().show();
            }
        });

        shaketoextendlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchshaketoextend.performClick();
            }
        });

        switchshaketoextend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settingsprefs.edit().putBoolean("shakeToExtend", isChecked).apply();
            }
        });

        shutdowncountdownlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchshutdowncountdown.performClick();
            }
        });

        switchshutdowncountdown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(getActivity())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                            builder.setTitle(getActivity().getString(R.string.enableshutdowncountdown));
                            builder.setMessage(getActivity().getString(R.string.shutdowncountdowndesc));
                            builder.setCancelable(false);

                            builder.setPositiveButton(getActivity().getString(R.string.enable), new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    checkDrawOverlayPermission();
                                    dialog.dismiss();
                                }
                            });

                            builder.setNegativeButton(getActivity().getString(R.string.nothanks), new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    settingsprefs.edit().putBoolean("sysOverlay", false).apply();
                                    switchshutdowncountdown.setChecked(false);
                                    dialog.dismiss();
                                }
                            });

                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {
                            settingsprefs.edit().putBoolean("sysOverlay", true).apply();
                        }
                    }
                } else {
                    settingsprefs.edit().putBoolean("sysOverlay", false).apply();
                }
            }
        });

        hapticHeadsUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchHapticHeadsUp.performClick();
            }
        });

        switchHapticHeadsUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settingsprefs.edit().putBoolean("hapticHeadsUpActive", isChecked).apply();
                if (isChecked) {
                    headsUpTime.setVisibility(View.VISIBLE);
                    headsUpTimeDivider.setVisibility(View.VISIBLE);
                } else {
                    if (!switchAcousticalHeadsUp.isChecked()) {
                        headsUpTime.setVisibility(View.GONE);
                        headsUpTimeDivider.setVisibility(View.GONE);
                    }
                }
            }
        });

        acousticalHeadsUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAcousticalHeadsUp.performClick();
            }
        });

        switchAcousticalHeadsUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settingsprefs.edit().putBoolean("acousticalHeadsUpActive", isChecked).apply();
                if (isChecked) {
                    headsUpSound.setVisibility(View.VISIBLE);
                    headsUpTime.setVisibility(View.VISIBLE);
                    headsUpTimeDivider.setVisibility(View.VISIBLE);
                    headsUpSoundDivider.setVisibility(View.VISIBLE);
                } else {
                    if (!switchHapticHeadsUp.isChecked()) {
                        headsUpTime.setVisibility(View.GONE);
                        headsUpTimeDivider.setVisibility(View.GONE);
                    }
                    headsUpSound.setVisibility(View.GONE);
                    headsUpSoundDivider.setVisibility(View.GONE);
                }
            }
        });

        headsUpTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final NumberPicker npView = (NumberPicker) inflater.inflate(R.layout.number_picker_dialog_layout, null);
                npView.setMinValue(1);
                npView.setMaxValue(10000);
                npView.setValue(settingsprefs.getInt("headsUpTime", 60000) / 60000);
                new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getActivity().getString(R.string.headsuptimetitle))
                        .setView(npView)
                        .setPositiveButton(getActivity().getString(R.string.save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        npView.clearFocus();
                                        settingsprefs.edit().putInt("headsUpTime", npView.getValue() * 60000).apply();
                                    }
                                })
                        .setNegativeButton(getActivity().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                    }
                                })
                        .create().show();
            }
        });

        headsUpSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] items = {getActivity().getString(R.string.sound) + " 1", getActivity().getString(R.string.sound) + " 2", getActivity().getString(R.string.sound) + " 3"};
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                final MediaPlayer mMediaPlayer = MediaPlayer.create(getActivity(), R.raw.sound1);
                final MediaPlayer mMediaPlayer2 = MediaPlayer.create(getActivity(), R.raw.sound2);
                final MediaPlayer mMediaPlayer3 = MediaPlayer.create(getActivity(), R.raw.sound3);
                builder.setTitle(getActivity().getString(R.string.selectheadsupsound));
                builder.setSingleChoiceItems(items, settingsprefs.getInt("shutdownSound", 1)-1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setLooping(false);
                        }
                        if (mMediaPlayer2 != null) {
                            mMediaPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer2.setLooping(false);
                        }
                        if (mMediaPlayer3 != null) {
                            mMediaPlayer3.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer3.setLooping(false);
                        }
                        switch (item) {
                            case 0:
                                headsUpSoundSelection = 1;
                                if (mMediaPlayer2 != null) {
                                    mMediaPlayer2.stop();
                                }
                                if (mMediaPlayer3 != null) {
                                    mMediaPlayer3.stop();
                                }
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.start();
                                }
                                break;
                            case 1:
                                headsUpSoundSelection = 2;
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.stop();
                                }
                                if (mMediaPlayer3 != null) {
                                    mMediaPlayer3.stop();
                                }
                                if (mMediaPlayer2 != null) {
                                    mMediaPlayer2.start();
                                }
                                break;
                            case 2:
                                headsUpSoundSelection = 3;
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.stop();
                                }
                                if (mMediaPlayer2 != null) {
                                    mMediaPlayer2.stop();
                                }
                                if (mMediaPlayer3 != null) {
                                    mMediaPlayer3.start();
                                }
                                break;
                        }
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        settingsprefs.edit().putInt("shutdownSound", headsUpSoundSelection).apply();
                        settingsprefs.edit().putBoolean("shutdownSoundChanged", true).apply();
                        if (mMediaPlayer != null) {
                            mMediaPlayer.stop();
                        }
                        if (mMediaPlayer2 != null) {
                            mMediaPlayer2.stop();
                        }
                        if (mMediaPlayer3 != null) {
                            mMediaPlayer3.stop();
                        }
                        levelDialog.dismiss();
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.stop();
                        }
                        if (mMediaPlayer2 != null) {
                            mMediaPlayer2.stop();
                        }
                        if (mMediaPlayer3 != null) {
                            mMediaPlayer3.stop();
                        }
                        levelDialog.dismiss();
                    }
                });
                levelDialog = builder.create();
                levelDialog.show();
            }
        });

        pinprotectionlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchpinprotection.performClick();
            }
        });

        switchpinprotection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent intent = new Intent(getActivity(), CustomPinActivity.class);
                    intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK);
                    startActivityForResult(intent, REQUEST_CODE_ENABLE);
                } else {
                    LockManager lockManager = LockManager.getInstance();
                    lockManager.getAppLock().disableAndRemoveConfiguration();
                    lockManager.getAppLock().setLogoId(R.mipmap.ic_launcher);
                    lockManager.getAppLock().setShouldShowForgot(false);
                    settingsprefs.edit().putBoolean("pinprotection", false).apply();
                    changepin.setVisibility(View.GONE);
                    changepindivider.setVisibility(View.GONE);
                }
            }
        });

        changepin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CustomPinActivity.class);
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.CHANGE_PIN);
                startActivityForResult(intent, REQUEST_CODE_ENABLE);
            }
        });

        contactlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: link to project on github (issues)
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(getActivity())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getActivity().getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences settingsprefs = getActivity().getSharedPreferences("settings", 0);
        SwitchCompat switchshutdowncountdown = (SwitchCompat) v.findViewById(R.id.switchshutdowncountdown);

        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(getActivity())) {
                // continue here - permission was granted
                settingsprefs.edit().putBoolean("sysOverlay", true).apply();
                switchshutdowncountdown.setChecked(true);
            } else {
                settingsprefs.edit().putBoolean("sysOverlay", false).apply();
                switchshutdowncountdown.setChecked(false);
            }
        } else if (requestCode == REQUEST_CODE_ENABLE) {
            settingsprefs.edit().putBoolean("pinprotection", true).apply();
            changepin.setVisibility(View.VISIBLE);
            changepindivider.setVisibility(View.VISIBLE);
        }
    }
}
