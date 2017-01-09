package de.jonathansautter.autooff;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CrashReporter extends Activity {

    private String logStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_crashreporter);

        final TextView log = (TextView) findViewById(R.id.log);
        TextView close = (TextView) findViewById(R.id.close);
        TextView send = (TextView) findViewById(R.id.send);
        final TextView showlog = (TextView) findViewById(R.id.showlog);
        final ImageView smiley = (ImageView) findViewById(R.id.smiley);
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        final RelativeLayout showloglayout = (RelativeLayout) findViewById(R.id.showloglayout);

        final Animation fade_in = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        final Animation fade_out = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        Intent intent = getIntent();
        if (intent != null) {
            logStr = intent.getStringExtra("error");
            if (logStr != null) {
                log.setText(logStr);
            } else {
                showlog.setText(R.string.unabletoreceiveerrorlog);
                send.setVisibility(View.GONE);
            }
        }

        showloglayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scrollView.isShown()) {
                    scrollView.startAnimation(fade_out);
                    scrollView.setVisibility(View.GONE);
                    AlphaAnimation animation1 = new AlphaAnimation(0.3f, 1.0f);
                    animation1.setDuration(700);
                    animation1.setFillAfter(true);
                    smiley.startAnimation(animation1);
                    showlog.setText(R.string.showlog);
                } else {
                    scrollView.startAnimation(fade_in);
                    scrollView.setVisibility(View.VISIBLE);
                    AlphaAnimation animation1 = new AlphaAnimation(1.0f, 0.3f);
                    animation1.setDuration(700);
                    animation1.setFillAfter(true);
                    smiley.startAnimation(animation1);
                    showlog.setText(R.string.hidelog);
                }
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashReporter.this.finish();
                Intent intent = new Intent(CrashReporter.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "info@jonathansautter.de", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.autooffbugreport));
                emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.autooffjustcrashed) + logStr);
                if (emailIntent.resolveActivity(getPackageManager()) == null) { // no email client installed
                    Toast.makeText(getApplicationContext(), R.string.noemailclient, Toast.LENGTH_LONG).show();
                } else {
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.sendemailvia)));
                    CrashReporter.this.finish();
                }
            }
        });
    }
}
