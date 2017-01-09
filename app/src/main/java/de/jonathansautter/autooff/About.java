package de.jonathansautter.autooff;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.about_layout);

        android.support.v7.app.ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.about);

        setup();
    }

    private void setup() {
        RelativeLayout author = (RelativeLayout) findViewById(R.id.authorly);
        RelativeLayout licences = (RelativeLayout) findViewById(R.id.licencesly);
        RelativeLayout bugreport = (RelativeLayout) findViewById(R.id.bugreportly);
        RelativeLayout fork = (RelativeLayout) findViewById(R.id.forkly);
        RelativeLayout rate = (RelativeLayout) findViewById(R.id.rately);
        RelativeLayout share = (RelativeLayout) findViewById(R.id.sharely);

        author.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://play.google.com/store/apps/dev?id=6736344768532679831");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        licences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Notices notices = new Notices();
                notices.addNotice(new Notice("CircularSeekBar", "https://github.com/devadvance/circularseekbar", "Copyright 2013 Matt Joseph", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("FloatingActionButton", "https://github.com/Clans/FloatingActionButton", "Copyright 2015 Dmytro Tarianyk", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("Android PagerSlidingTabStrip", "https://github.com/jpardogo/PagerSlidingTabStrip", "Copyright 2013 Andreas Stuetz", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("LolliPin", "https://github.com/OrangeGangsters/LolliPin", "he MIT License (MIT)\n\nCopyright (c) 2015 OrangeGangsters", new MITLicense()));

                new LicensesDialog.Builder(About.this)
                        .setNotices(notices)
                        .setIncludeOwnLicense(true)
                        .build()
                        .show();
            }
        });

        bugreport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://github.com/jonathansautter/AutoOff/issues";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        fork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://github.com/jonathansautter/AutoOff";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent shareintent = new Intent(Intent.ACTION_SEND);
                    shareintent.setType("text/plain");
                    shareintent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
                    String content = getString(R.string.sharetext) + getPackageName();
                    shareintent.putExtra(Intent.EXTRA_TEXT, content);
                    startActivity(Intent.createChooser(shareintent, getString(R.string.sharevia)));
                } catch (Exception e) {
                    Toast.makeText(About.this, R.string.somethingwentwrong, Toast.LENGTH_LONG).show();
                }
            }
        });
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
