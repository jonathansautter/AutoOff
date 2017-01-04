package de.jonathansautter.autooff;

import android.app.Application;

import com.github.orangegangsters.lollipin.lib.managers.LockManager;

public class AutoOff extends Application {

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate() {
        super.onCreate();

        LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
        lockManager.enableAppLock(this, CustomPinActivity.class);
        lockManager.getAppLock().setLogoId(R.mipmap.ic_launcher);
        lockManager.getAppLock().setShouldShowForgot(false);
    }
}