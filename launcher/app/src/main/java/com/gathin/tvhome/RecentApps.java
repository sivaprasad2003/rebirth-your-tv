package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

    static boolean isExcluded(String pkg) {
        if (pkg == null || pkg.isEmpty()) return true;
        String lower = pkg.toLowerCase();

        // System core components
        if (lower.equals("android") || lower.startsWith("com.android.") || lower.startsWith("com.google.android.")) {
            // Keep user apps like YouTube even if under google domain
            if (!lower.equals("com.google.android.youtube") && !lower.equals("com.google.android.youtube.tv")) {
                return true;
            }
        }

        // Hardware / Vendor / OEM background daemons
        if (lower.startsWith("com.mediatek.") || lower.startsWith("com.cvte.")
                || lower.contains("mitv") || lower.contains("smartshare")
                || lower.contains("wifidisplay") || lower.contains("wfd")
                || lower.startsWith("mitv.service")) {
            return true;
        }

        // System utilities & hardware players/tuners (MediaPlayer, LiveTV, Miracast, AirPlay, Gallery)
        if (lower.contains("livetv") || lower.contains("mediaexplorer") || lower.contains("gallery")
                || lower.contains("miracast") || lower.contains("airplay") || lower.contains("tvinput")
                || lower.contains("tuner") || lower.contains("mediashell") || lower.contains("systemui")
                || lower.contains("inputmethod") || lower.contains("keyguard")) {
            return true;
        }

        return false;
    }

    static boolean isLabelExcluded(CharSequence label) {
        if (label == null) return false;
        String lower = label.toString().toLowerCase();
        return lower.contains("miracast") || lower.contains("live tv") || lower.contains("gallery")
                || lower.contains("media player") || lower.contains("media explorer")
                || lower.contains("tv manager") || lower.contains("system updater");
    }

    static void record(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.equals(ctx.getPackageName()) || isExcluded(pkg)) return;
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
                if (pkg.isEmpty() || pkg.equals(ctx.getPackageName()) || isExcluded(pkg)) continue;
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    CharSequence label = pm.getApplicationLabel(info);
                    if (isLabelExcluded(label)) continue;
                    Drawable icon = pm.getApplicationIcon(info);
                    entries.add(new Entry(pkg, label != null ? label.toString() : pkg, icon));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // App uninstalled
                }
            }
        }

        // Fallback only if no recent history: discover user-launchable Leanback apps (never system daemons)
        if (entries.isEmpty()) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
            if (resolveInfos != null) {
                for (ResolveInfo ri : resolveInfos) {
                    String pkg = ri.activityInfo.packageName;
                    if (pkg != null && !pkg.equals(ctx.getPackageName()) && !isExcluded(pkg)) {
                        CharSequence label = ri.loadLabel(pm);
                        if (isLabelExcluded(label)) continue;
                        entries.add(new Entry(pkg, label.toString(), ri.loadIcon(pm)));
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
            try {
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (ActivityManager.RunningAppProcessInfo p : procs) {
                        if (p.pkgList != null) {
                            for (String pPkg : p.pkgList) {
                                if (pkg.equals(pPkg)) {
                                    am.killBackgroundProcesses(pPkg);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
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
        if (am != null) {
            try {
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (ActivityManager.RunningAppProcessInfo p : procs) {
                        if (p.pkgList != null) {
                            for (String pkg : p.pkgList) {
                                if (!pkg.equals(ctx.getPackageName())) {
                                    am.killBackgroundProcesses(pkg);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_RECENTS, "");
            if (!raw.isEmpty()) {
                String[] pkgs = raw.split(",");
                for (String pkg : pkgs) {
                    pkg = pkg.trim();
                    if (!pkg.isEmpty() && !pkg.equals(ctx.getPackageName())) {
                        am.killBackgroundProcesses(pkg);
                    }
                }
            }
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_RECENTS).apply();
    }
}
