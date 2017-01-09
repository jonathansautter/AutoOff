package de.jonathansautter.autooff;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.PrintWriter;
import java.io.StringWriter;

class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Activity myContext;

    ExceptionHandler(Activity context) {
        myContext = context;
    }

    public void uncaughtException(Thread thread, Throwable exception) {

        String LINE_SEPARATOR = "\n";

        PackageInfo pInfo = null;
        try {
            pInfo = myContext.getPackageManager().getPackageInfo(myContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        StringBuilder errorReport = new StringBuilder();
        errorReport.append("* CAUSE OF ERROR *\n");
        errorReport.append(stackTrace.toString());
        errorReport.append("\n* DEVICE INFORMATION *\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n* FIRMWARE *\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK_INT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        if (pInfo != null) {
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("\n* APP *\n");
            errorReport.append("Version Code: ");
            errorReport.append(pInfo.versionCode);
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Version Name: ");
            errorReport.append(pInfo.versionName);
        }

        Intent intent = new Intent(myContext, CrashReporter.class);
        intent.putExtra("error", errorReport.toString());
        myContext.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}