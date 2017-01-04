package de.jonathansautter.autooff;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.github.orangegangsters.lollipin.lib.managers.AppLock;
import com.github.orangegangsters.lollipin.lib.managers.LockManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    static boolean main_active = false;
    private SharedPreferences settingsprefs;
    public static PageAdapter pageAdapter;

    @Override
    public void onStart() {
        super.onStart();
        main_active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        main_active = false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onResume() {
        super.onResume();
        LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
        //Log.d("AutoOff", "should lock: "+lockManager.getAppLock().shouldLockSceen(MainActivity.this));

        // pin protection
        if (lockManager.getAppLock().isPasscodeSet() && lockManager.getAppLock().shouldLockSceen(MainActivity.this)) {
            Intent intent = new Intent(MainActivity.this, CustomPinActivity.class);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_main);

        setTitle(R.string.app_name);

        settingsprefs = getSharedPreferences("settings", 0);

        if (!settingsprefs.getBoolean("disclaimerAgreed", false)) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
            builder1.setTitle(getString(R.string.welcome));

            SpannableString s = new SpannableString(getString(R.string.roothint));
            Linkify.addLinks(s, Linkify.ALL);

            builder1.setMessage(s);
            builder1.setCancelable(false);

            builder1.setPositiveButton(getString(R.string.imrooted), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(getString(R.string.disclaimer));
                    builder.setMessage(getString(R.string.disclaimertext));
                    builder.setCancelable(false);

                    builder.setPositiveButton(getString(R.string.agree), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            settingsprefs.edit().putBoolean("disclaimerAgreed", true).apply();
                            dialog.dismiss();
                            sysoverlaypermission();
                        }
                    });

                    builder.setNegativeButton(getString(R.string.disagree), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });

            builder1.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    MainActivity.this.finish();
                }
            });

            AlertDialog alert1 = builder1.create();
            alert1.show();
            ((TextView) alert1.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            sysoverlaypermission();
        }

        //placeRebootFile();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        // Initialize the ViewPager and set an adapter
        ViewPager pager = (ViewPager) findViewById(R.id.viewpager);
        pageAdapter = new PageAdapter(getSupportFragmentManager(), MainActivity.this);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        int lastUsedTab = settingsprefs.getInt("lastUsedTab", 0);
        if (pager != null) {
            pager.setOffscreenPageLimit(4);
            pager.setAdapter(pageAdapter);

            // Bind the tabs to the ViewPager
            if (tabs != null) {
                tabs.setViewPager(pager);
                pager.setPageMargin(pageMargin);
                pager.setCurrentItem(lastUsedTab);

                tabs.setOnTabReselectedListener(new PagerSlidingTabStrip.OnTabReselectedListener() {
                    @Override
                    public void onTabReselected(int position) {

                    }
                });
            }
        }

        if (getIntent().getBooleanExtra("shutdownerror", false)) {
            // show message: sorry unable to shutdown your device! root access granted? try busybox!?
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AppCompatAlertDialogStyle);
            builder.setTitle(getString(R.string.weresorry));
            builder.setMessage(getString(R.string.unabletoshutdown));
            builder.setCancelable(false);

            builder.setPositiveButton(getString(R.string.exit), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    MainActivity.this.finish();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void sysoverlaypermission() {
        if (settingsprefs.getBoolean("firstLaunch", true)) {
            settingsprefs.edit().putBoolean("firstLaunch", false).apply();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(getApplicationContext())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.enableshutdowncountdown);
                    builder.setMessage(R.string.shutdowncountdowndesc);
                    builder.setCancelable(false);

                    builder.setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            checkDrawOverlayPermission();
                            dialog.dismiss();
                        }
                    });

                    builder.setNegativeButton(R.string.nothanks, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            settingsprefs.edit().putBoolean("sysOverlay", false).apply();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    settingsprefs.edit().putBoolean("sysOverlay", true).apply();
                }
            }
        } else if (settingsprefs.getBoolean("sysOverlay", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(getApplicationContext())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                    builder.setTitle(R.string.enableshutdowncountdown);
                    builder.setMessage(getString(R.string.reenablecountdowndesc));
                    builder.setCancelable(false);

                    builder.setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            checkDrawOverlayPermission();
                            dialog.dismiss();
                        }
                    });

                    builder.setNegativeButton(R.string.nothanks, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            settingsprefs.edit().putBoolean("sysOverlay", false).apply();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    settingsprefs.edit().putBoolean("sysOverlay", true).apply();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // continue here - permission was granted
                settingsprefs.edit().putBoolean("sysOverlay", true).apply();
                Settings_Fragment fragment = (Settings_Fragment) pageAdapter.getRegisteredFragment(3);
                if (fragment != null) {
                    fragment.setup();
                }
            } else {
                settingsprefs.edit().putBoolean("sysOverlay", false).apply();
            }
        }
    }

    public void refreshTabs() {
        Minute_Fragment minute_fragment = (Minute_Fragment) pageAdapter.getRegisteredFragment(0);
        Boot_Fragment boot_fragment = (Boot_Fragment) pageAdapter.getRegisteredFragment(2);
        minute_fragment.refresh();
        boot_fragment.refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.rate:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent rateintent = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                rateintent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(rateintent);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                return true;
            case R.id.share:
                try {
                    Intent shareintent = new Intent(Intent.ACTION_SEND);
                    shareintent.setType("text/plain");
                    shareintent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
                    String content = getString(R.string.sharetext) + getPackageName();
                    shareintent.putExtra(Intent.EXTRA_TEXT, content);
                    startActivity(Intent.createChooser(shareintent, getString(R.string.sharevia)));
                } catch (Exception e) {
                    Toast.makeText(this, R.string.somethingwentwrong, Toast.LENGTH_LONG).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}