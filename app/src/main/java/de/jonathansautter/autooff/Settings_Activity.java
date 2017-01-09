package de.jonathansautter.autooff;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import com.github.orangegangsters.lollipin.lib.managers.AppLock;
import com.github.orangegangsters.lollipin.lib.managers.LockManager;

public class Settings_Activity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_ENABLE = 1532;
    private SharedPreferences settingsprefs;
    private RelativeLayout changepin;
    private FrameLayout changepindivider;
    private FrameLayout headsUpTimeDivider;
    private FrameLayout headsUpSoundDivider;
    AlertDialog levelDialog;
    private int headsUpSoundSelection = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.settings_layout);

        android.support.v7.app.ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.settings);

        settingsprefs = getSharedPreferences("settings", 0);

        setup();
    }

    public void setup() {

        RelativeLayout maxminuteslayout = (RelativeLayout) findViewById(R.id.maxminutes);
        RelativeLayout extensiontimelayout = (RelativeLayout) findViewById(R.id.extensiontime);
        RelativeLayout shaketoextendlayout = (RelativeLayout) findViewById(R.id.shaketoextend);
        final SwitchCompat switchshaketoextend = (SwitchCompat) findViewById(R.id.switchshaketoextend);
        RelativeLayout shutdowncountdownlayout = (RelativeLayout) findViewById(R.id.shutdowncountdown);
        final SwitchCompat switchshutdowncountdown = (SwitchCompat) findViewById(R.id.switchshutdowncountdown);
        RelativeLayout pinprotectionlayout = (RelativeLayout) findViewById(R.id.pinprotection);
        final SwitchCompat switchpinprotection = (SwitchCompat) findViewById(R.id.switchpinprotection);
        RelativeLayout acousticalHeadsUp = (RelativeLayout) findViewById(R.id.acousticalHeadsUp);
        RelativeLayout hapticHeadsUp = (RelativeLayout) findViewById(R.id.hapticHeadsUp);
        final SwitchCompat switchAcousticalHeadsUp = (SwitchCompat) findViewById(R.id.switchAcousticalHeadsUp);
        final SwitchCompat switchHapticHeadsUp = (SwitchCompat) findViewById(R.id.switchHapticHeadsUp);
        final RelativeLayout headsUpSound = (RelativeLayout) findViewById(R.id.headsUpSound);
        final RelativeLayout headsUpTime = (RelativeLayout) findViewById(R.id.headsUpTime);
        changepin = (RelativeLayout) findViewById(R.id.changepin);
        changepindivider = (FrameLayout) findViewById(R.id.changepindivider);
        headsUpTimeDivider = (FrameLayout) findViewById(R.id.headsUpTimeDivider);
        headsUpSoundDivider = (FrameLayout) findViewById(R.id.headsUpSoundDivider);

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

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            // device has accelerometer
            shaketoextendlayout.setVisibility(View.VISIBLE);
            switchshaketoextend.setChecked(settingsprefs.getBoolean("shakeToExtend", false));
        } else {
            shaketoextendlayout.setVisibility(View.GONE);
            if (settingsprefs.getBoolean("shakeToExtend", false)) {
                settingsprefs.edit().putBoolean("shakeToExtend", false).apply();
            }
        }

        Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Check whether device hardware has a Vibrator
        if (mVibrator.hasVibrator()) {
            hapticHeadsUp.setVisibility(View.VISIBLE);
            switchHapticHeadsUp.setChecked(settingsprefs.getBoolean("hapticHeadsUpActive", false));
        } else {
            hapticHeadsUp.setVisibility(View.GONE);
            if (settingsprefs.getBoolean("hapticHeadsUpActive", false)) {
                settingsprefs.edit().putBoolean("hapticHeadsUpActive", false).apply();
            }
        }

        //initiate();

        maxminuteslayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater)
                        getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final NumberPicker npView = (NumberPicker) inflater.inflate(R.layout.number_picker_dialog_layout, null);
                npView.setMinValue(1);
                npView.setMaxValue(10000);
                npView.setValue(settingsprefs.getInt("maxminutes", 60));
                new AlertDialog.Builder(Settings_Activity.this, R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.maxminutes))
                        .setView(npView)
                        .setPositiveButton(getString(R.string.save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        npView.clearFocus();
                                        settingsprefs.edit().putInt("maxminutes", npView.getValue()).apply();
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel),
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
                        getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final NumberPicker npView = (NumberPicker) inflater.inflate(R.layout.number_picker_dialog_layout, null);
                npView.setMinValue(1);
                npView.setMaxValue(10000);
                npView.setValue(settingsprefs.getInt("extensiontime", 10));
                new AlertDialog.Builder(Settings_Activity.this, R.style.AppCompatAlertDialogStyle)
                        .setTitle(R.string.extensiontime)
                        .setView(npView)
                        .setPositiveButton(getString(R.string.save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        npView.clearFocus();
                                        settingsprefs.edit().putInt("extensiontime", npView.getValue()).apply();
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel),
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
                        if (!android.provider.Settings.canDrawOverlays(Settings_Activity.this)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(Settings_Activity.this, R.style.AppCompatAlertDialogStyle);
                            builder.setTitle(getString(R.string.enableshutdowncountdown));
                            builder.setMessage(getString(R.string.shutdowncountdowndesc));
                            builder.setCancelable(false);

                            builder.setPositiveButton(getString(R.string.enable), new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    checkDrawOverlayPermission();
                                    dialog.dismiss();
                                }
                            });

                            builder.setNegativeButton(getString(R.string.nothanks), new DialogInterface.OnClickListener() {

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
                        getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final NumberPicker npView = (NumberPicker) inflater.inflate(R.layout.number_picker_dialog_layout, null);
                npView.setMinValue(1);
                npView.setMaxValue(10000);
                npView.setValue(settingsprefs.getInt("headsUpTime", 60000) / 60000);
                new AlertDialog.Builder(Settings_Activity.this, R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.headsuptimetitle))
                        .setView(npView)
                        .setPositiveButton(getString(R.string.save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        npView.clearFocus();
                                        settingsprefs.edit().putInt("headsUpTime", npView.getValue() * 60000).apply();
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel),
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
                final CharSequence[] items = {getString(R.string.sound) + " 1", getString(R.string.sound) + " 2", getString(R.string.sound) + " 3"};
                final AlertDialog.Builder builder = new AlertDialog.Builder(Settings_Activity.this, R.style.AppCompatAlertDialogStyle);
                final MediaPlayer mMediaPlayer = MediaPlayer.create(Settings_Activity.this, R.raw.sound1);
                final MediaPlayer mMediaPlayer2 = MediaPlayer.create(Settings_Activity.this, R.raw.sound2);
                final MediaPlayer mMediaPlayer3 = MediaPlayer.create(Settings_Activity.this, R.raw.sound3);
                builder.setTitle(getString(R.string.selectheadsupsound));
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
                    Intent intent = new Intent(Settings_Activity.this, CustomPinActivity.class);
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
                Intent intent = new Intent(Settings_Activity.this, CustomPinActivity.class);
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.CHANGE_PIN);
                startActivityForResult(intent, REQUEST_CODE_ENABLE);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(Settings_Activity.this)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SwitchCompat switchshutdowncountdown = (SwitchCompat) findViewById(R.id.switchshutdowncountdown);

        if (requestCode == REQUEST_CODE) {
            if (android.provider.Settings.canDrawOverlays(Settings_Activity.this)) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
