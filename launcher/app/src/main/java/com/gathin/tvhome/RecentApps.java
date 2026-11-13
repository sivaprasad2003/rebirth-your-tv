package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tracks recently opened apps and provides background process termination to reclaim RAM.
 */
final class RecentApps {
    private static final String PREF_NAME = "tvhome_recent_apps";
    private static final String KEY_RECENTS = "recent_packages";
    private static final int MAX_RECENTS = 10;

    static final class Entry {
        final String packageName;
        final String label;
        final Drawable icon;

        Entry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    static void record(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.equals(ctx.getPackageName())) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENTS, "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            list.addAll(Arrays.asList(raw.split(",")));
        }
        list.remove(pkg);
        list.add(0, pkg);
        while (list.size() > MAX_RECENTS) {
            list.remove(list.size() - 1);
        }
        prefs.edit().putString(KEY_RECENTS, String.join(",", list)).apply();
    }

    static List<Entry> getRecentEntries(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENTS, "");
        PackageManager pm = ctx.getPackageManager();
        List<Entry> entries = new ArrayList<>();

        if (!raw.isEmpty()) {
            String[] pkgs = raw.split(",");
            for (String pkg : pkgs) {
                pkg = pkg.trim();
                if (pkg.isEmpty() || pkg.equals(ctx.getPackageName())) continue;
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    CharSequence label = pm.getApplicationLabel(info);
                    Drawable icon = pm.getApplicationIcon(info);
                    entries.add(new Entry(pkg, label != null ? label.toString() : pkg, icon));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // App uninstalled
                }
            }
        }

        // Fallback if no recent history: discover running third-party processes
        if (entries.isEmpty()) {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (ActivityManager.RunningAppProcessInfo p : procs) {
                        if (p.pkgList != null) {
                            for (String pkg : p.pkgList) {
                                if (!pkg.equals(ctx.getPackageName()) && pm.getLaunchIntentForPackage(pkg) != null) {
                                    try {
                                        ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                                        entries.add(new Entry(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info)));
                                        if (entries.size() >= 5) break;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        if (entries.size() >= 5) break;
                    }
                }
            }
        }

        return entries;
    }

    static void kill(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) return;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.killBackgroundProcesses(pkg);
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENTS, "");
        if (!raw.isEmpty()) {
            List<String> list = new ArrayList<>(Arrays.asList(raw.split(",")));
            list.remove(pkg);
            prefs.edit().putString(KEY_RECENTS, String.join(",", list)).apply();
        }
    }

    static void killAll(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENTS, "");
        if (!raw.isEmpty()) {
            String[] pkgs = raw.split(",");
            if (am != null) {
                for (String pkg : pkgs) {
                    am.killBackgroundProcesses(pkg.trim());
                }
            }
        }
        prefs.edit().remove(KEY_RECENTS).apply();
    }
}
