package com.gathin.tvhome;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Task switcher and memory cleaner: displays recent applications in Leanback glass cards,
 * allowing instant app switching and 1-click background task termination to reclaim RAM.
 */
public final class RecentsActivity extends Activity {

    private TextView ramStats;
    private LinearLayout container;
    private View emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recents);

        findViewById(R.id.recentsScrim).setOnClickListener(v -> finish());
        ramStats = findViewById(R.id.recentsRamStats);
        container = findViewById(R.id.recentsContainer);
        emptyText = findViewById(R.id.emptyRecentsText);

        findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            RecentApps.killAll(this);
            container.removeAllViews();
            emptyText.setVisibility(View.VISIBLE);
            updateRamStats();
            v.postDelayed(this::updateRamStats, 400);
            v.postDelayed(this::updateRamStats, 1000);
            Toast.makeText(this, R.string.all_tasks_killed, Toast.LENGTH_SHORT).show();
        });

        populateRecents();
        updateRamStats();
    }

    private void updateRamStats() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long availMb = mi.availMem / (1024 * 1024);
            long totalMb = mi.totalMem / (1024 * 1024);
            ramStats.setText(getString(R.string.ram_status, availMb, totalMb));
        }
    }

    private void populateRecents() {
        container.removeAllViews();
        List<RecentApps.Entry> entries = RecentApps.getRecentEntries(this);
        if (entries.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }
        emptyText.setVisibility(View.GONE);

        for (RecentApps.Entry entry : entries) {
            View card = getLayoutInflater().inflate(R.layout.item_recent_app, container, false);
            ((ImageView) card.findViewById(R.id.appIcon)).setImageDrawable(entry.icon);
            ((TextView) card.findViewById(R.id.appLabel)).setText(entry.label);

            // Click / OK: Switch to app
            card.setOnClickListener(v -> {
                Intent launch = getPackageManager().getLaunchIntentForPackage(entry.packageName);
                if (launch != null) {
                    finish();
                    try {
                        startActivity(launch);
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // DPAD_UP: Kill app
            card.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    killCard(card, entry);
                    return true;
                }
                return false;
            });

            // Click on kill button
            card.findViewById(R.id.btnKillApp).setOnClickListener(v -> killCard(card, entry));

            container.addView(card);
        }

        if (container.getChildCount() > 0) {
            container.getChildAt(0).requestFocus();
        }
    }

    private void killCard(View card, RecentApps.Entry entry) {
        RecentApps.kill(this, entry.packageName);
        card.animate().alpha(0f).translationY(-40f).setDuration(180).withEndAction(() -> {
            container.removeView(card);
            if (container.getChildCount() == 0) {
                emptyText.setVisibility(View.VISIBLE);
            } else {
                container.getChildAt(0).requestFocus();
            }
            updateRamStats();
            if (ramStats != null) {
                ramStats.postDelayed(this::updateRamStats, 400);
                ramStats.postDelayed(this::updateRamStats, 1000);
            }
        }).start();
        Toast.makeText(this, getString(R.string.task_killed, entry.label), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }
}
