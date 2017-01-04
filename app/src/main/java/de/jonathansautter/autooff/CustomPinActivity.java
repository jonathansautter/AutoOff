package de.jonathansautter.autooff;

import com.github.orangegangsters.lollipin.lib.managers.AppLockActivity;

public class CustomPinActivity extends AppLockActivity {

    @Override
    public int getContentView() {
        return R.layout.activity_pin_code;
    }

    @Override
    public void showForgotDialog() {
        // forgot password behaviour (no forgot pass function? just clear app data, user can uninstall app anyway)
        // reset pin:
        // AppLock appLock = LockManager.getInstance().getAppLock(); if (appLock != null) { appLock.disableAndRemoveConfiguration(); }
    }

    @Override
    public void onPinFailure(int attempts) {

    }

    @Override
    public void onPinSuccess(int attempts) {

    }

    @Override
    public int getPinLength() {
        return super.getPinLength();//you can override this method to change the pin length from the default 4
    }

}
