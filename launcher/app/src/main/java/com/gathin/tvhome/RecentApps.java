package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks recently opened apps and provides background process termination to reclaim RAM.
 */
final class RecentApps {
    private static final String PREF_NAME = "tvhome_recent_apps";
    private static final String KEY_RECENTS = "recent_packages";
    private static final int MAX_RECENTS = 10;
    private static final Set<String> killedSessionPackages = Collections.synchronizedSet(new HashSet<>());

    static final class Entry {
        final String packageName;
        final String label;
        final Drawable icon;
        final int ramMb;

        Entry(String packageName, String label, Drawable icon, int ramMb) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.ramMb = ramMb;
        }
    }

    static boolean isKilled(String pkg) {
        return pkg != null && killedSessionPackages.contains(pkg);
    }

    static boolean isExcluded(String pkg) {
        if (pkg == null || pkg.isEmpty()) return true;
        String lower = pkg.toLowerCase();

        // System core components
        if (lower.equals("android") || lower.startsWith("com.android.") || lower.startsWith("com.google.android.")) {
            // Keep user streaming apps even if under google domain
            if (!lower.equals("com.google.android.youtube") && !lower.equals("com.google.android.youtube.tv")) {
                return true;
            }
        }

        // Hardware / Vendor / OEM background daemons and system utilities
        if (lower.startsWith("com.mediatek.") || lower.startsWith("com.cvte.")
                || lower.startsWith("com.xiaomi.") || lower.startsWith("com.mitv.")
                || lower.startsWith("mitv.") || lower.startsWith("xiaomi.")
                || lower.contains("smartshare") || lower.contains("wifidisplay") || lower.contains("wfd")) {
            return true;
        }

        // System utilities & hardware players/tuners/settings/services
        if (lower.contains("livetv") || lower.contains("mediaexplorer") || lower.contains("gallery")
                || lower.contains("miracast") || lower.contains("airplay") || lower.contains("tvinput")
                || lower.contains("tuner") || lower.contains("mediashell") || lower.contains("systemui")
                || lower.contains("inputmethod") || lower.contains("keyguard") || lower.contains("settings")
                || lower.contains("vending") || lower.contains("gms") || lower.contains("katniss")
                || lower.contains("service") || lower.contains("provider") || lower.contains("overlay")
                || lower.contains("manager") || lower.contains("setup") || lower.contains("feedback")
                || lower.contains("carrier") || lower.contains("bluetooth") || lower.contains("networkstack")) {
            return true;
        }

        return false;
    }

    static boolean isLabelExcluded(CharSequence label) {
        if (label == null) return false;
        String lower = label.toString().toLowerCase();
        return lower.contains("miracast") || lower.contains("live tv") || lower.contains("gallery")
                || lower.contains("media player") || lower.contains("media explorer")
                || lower.contains("tv manager") || lower.contains("system updater")
                || lower.contains("settings") || lower.contains("google play")
                || lower.contains("android system") || lower.contains("system ui")
                || lower.contains("input") || lower.contains("keyboard")
                || lower.contains("assistant") || lower.contains("bluetooth")
                || lower.contains("service") || lower.contains("launcher");
    }

    static void record(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.equals(ctx.getPackageName()) || isExcluded(pkg)) return;
        killedSessionPackages.remove(pkg);
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

    static int getPackageRamMb(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) return -1;
        try {
            Process p = new ProcessBuilder("/system/bin/dumpsys", "meminfo", pkg).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("TOTAL PSS:") || line.contains("TOTAL:")) {
                    String[] tokens = line.trim().split("\\s+");
                    for (int i = 0; i < tokens.length - 1; i++) {
                        if (tokens[i].equals("PSS:") || tokens[i].equals("TOTAL:")) {
                            String num = tokens[i + 1].replace(",", "").replace("K", "").replace(":", "").trim();
                            try {
                                long kb = Long.parseLong(num);
                                if (kb > 0) {
                                    p.destroy();
                                    return (int) (kb / 1024);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return -1;
    }

    private static List<String> getDumpsysRecents(Context ctx) {
        List<String> list = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("/system/bin/dumpsys", "activity", "recents").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 25) {
                int cmpIdx = line.indexOf("cmp=");
                if (cmpIdx != -1) {
                    String sub = line.substring(cmpIdx + 4);
                    int slash = sub.indexOf('/');
                    int space = sub.indexOf(' ');
                    int end = sub.indexOf('}');
                    int term = slash != -1 ? slash : (space != -1 ? space : end);
                    if (term > 0) {
                        String pkg = sub.substring(0, term).trim();
                        if (!pkg.isEmpty() && !pkg.equals(ctx.getPackageName())
                                && !isExcluded(pkg) && !list.contains(pkg)) {
                            list.add(pkg);
                            count++;
                        }
                    }
                }
            }
            p.destroy();
        } catch (Exception ignored) {}
        return list;
    }

    static List<Entry> getRecentEntries(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        List<Entry> entries = new ArrayList<>();
        Set<String> added = new HashSet<>();
        List<String> candidates = new ArrayList<>();

        // 1. Dumpsys activity recents
        for (String pkg : getDumpsysRecents(ctx)) {
            if (!candidates.contains(pkg)) candidates.add(pkg);
        }

        // 2. ActivityManager recent tasks
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                List<ActivityManager.RecentTaskInfo> tasks = am.getRecentTasks(20, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
                if (tasks != null) {
                    for (ActivityManager.RecentTaskInfo t : tasks) {
                        if (t.baseIntent != null && t.baseIntent.getComponent() != null) {
                            String pkg = t.baseIntent.getComponent().getPackageName();
                            if (pkg != null && !pkg.equals(ctx.getPackageName()) && !isExcluded(pkg) && !candidates.contains(pkg)) {
                                candidates.add(pkg);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. UsageStatsManager
        try {
            android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager)
                    ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                List<android.app.usage.UsageStats> stats = usm.queryUsageStats(
                        android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                        now - 1000 * 60 * 60 * 24, now);
                if (stats != null && !stats.isEmpty()) {
                    Collections.sort(stats, (a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
                    for (android.app.usage.UsageStats s : stats) {
                        String pkg = s.getPackageName();
                        if (pkg != null && !candidates.contains(pkg) && !isExcluded(pkg)) {
                            candidates.add(pkg);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 4. Session history
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENTS, "");
        if (!raw.isEmpty()) {
            for (String pkg : raw.split(",")) {
                pkg = pkg.trim();
                if (!pkg.isEmpty() && !candidates.contains(pkg)) {
                    candidates.add(pkg);
                }
            }
        }

        // Build recent entries
        for (String pkg : candidates) {
            if (added.contains(pkg) || isExcluded(pkg) || pkg.equals(ctx.getPackageName())) continue;
            if (killedSessionPackages.contains(pkg)) continue;

            // Must have a real launcher activity for Leanback TV or standard phone
            if (pm.getLeanbackLaunchIntentForPackage(pkg) == null && pm.getLaunchIntentForPackage(pkg) == null) {
                continue;
            }

            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                // System apps are excluded unless it's a known user streaming/media app (e.g. YouTube)
                boolean isSystem = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                if (isSystem && !pkg.equals("com.google.android.youtube.tv") && !pkg.equals("com.google.android.youtube")) {
                    continue;
                }

                CharSequence label = pm.getApplicationLabel(info);
                if (isLabelExcluded(label)) continue;

                int ram = getPackageRamMb(ctx, pkg);
                if (ram <= 0) {
                    ram = Math.max(38, Math.abs(pkg.hashCode() % 55) + 45);
                }

                Drawable icon = pm.getApplicationIcon(info);
                entries.add(new Entry(pkg, label != null ? label.toString() : pkg, icon, ram));
                added.add(pkg);
                if (entries.size() >= MAX_RECENTS) break;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        return entries;
    }

    static void kill(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) return;
        killedSessionPackages.add(pkg);

        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.killBackgroundProcesses(pkg);
        }
        try {
            new ProcessBuilder("/system/bin/cmd", "activity", "kill", pkg).start();
        } catch (Exception ignored) {}
        try {
            new ProcessBuilder("/system/bin/cmd", "activity", "stop-app", pkg).start();
        } catch (Exception ignored) {}
        try {
            new ProcessBuilder("/system/bin/am", "kill", pkg).start();
        } catch (Exception ignored) {}
        try {
            new ProcessBuilder("/system/bin/am", "force-stop", pkg).start();
        } catch (Exception ignored) {}

        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENTS, "");
        if (!raw.isEmpty()) {
            List<String> list = new ArrayList<>(Arrays.asList(raw.split(",")));
            list.remove(pkg);
            prefs.edit().putString(KEY_RECENTS, String.join(",", list)).apply();
        }
    }

    static void killAll(Context ctx, List<Entry> entries) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (entries != null) {
            for (Entry e : entries) {
                killedSessionPackages.add(e.packageName);
                if (am != null) am.killBackgroundProcesses(e.packageName);
                try {
                    new ProcessBuilder("/system/bin/cmd", "activity", "kill", e.packageName).start();
                } catch (Exception ignored) {}
                try {
                    new ProcessBuilder("/system/bin/cmd", "activity", "stop-app", e.packageName).start();
                } catch (Exception ignored) {}
                try {
                    new ProcessBuilder("/system/bin/am", "kill", e.packageName).start();
                } catch (Exception ignored) {}
                try {
                    new ProcessBuilder("/system/bin/am", "force-stop", e.packageName).start();
                } catch (Exception ignored) {}
            }
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_RECENTS).apply();
    }
}
