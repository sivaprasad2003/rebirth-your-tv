package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView ambientGlowChoice;
    private TextView dockGlassChoice;
    private TextView showStatsChoice;
    private TextView showBluetoothChoice;
    private TextView showInputsChoice;
    private TextView showNetworkChoice;
    private TextView wifiStatusColorChoice;
    private TextView iconOnlyButtonsChoice;
    private TextView cinematicChoice;
    private TextView fadeRowsChoice;
    private TextView settingsKeyPanelChoice;
    private TextView festivalChoice;
    private TextView tileMenuChoice;
    private TextView screensaverChoice;
    private TextView remapNetflixChoice;
    private TextView remapPrimeChoice;
    private TextView remapHotstarChoice;
    private TextView remapSonyLivChoice;
    private TextView remapLiveTvChoice;
    private TextView followThemeChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;
    private boolean ambientGlow;
    private boolean dockGlass;
    private boolean showStats;
    private boolean showBluetoothPill;
    private boolean showInputsPill;
    private boolean showNetworkPill;
    private boolean wifiStatusColor;
    private boolean iconOnlyButtons;
    private boolean cinematicHome;
    private boolean fadeRows;
    private boolean settingsKeyPanel;
    private boolean festivalDiwali;
    private boolean tileMenu;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        ambientGlow = config.ambientGlow;
        dockGlass = config.dockGlass;
        showStats = config.showStats;
        showBluetoothPill = config.showBluetoothPill;
        showInputsPill = config.showInputsPill;
        showNetworkPill = config.showNetworkPill;
        wifiStatusColor = config.wifiStatusColor;
        iconOnlyButtons = config.iconOnlyButtons;
        cinematicHome = config.cinematicHome;
        fadeRows = config.fadeRows;
        settingsKeyPanel = config.settingsKeyPanel;
        festivalDiwali = config.festival != null && "diwali".equalsIgnoreCase(config.festival.name);
        tileMenu = config.tileMenu;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        ambientGlowChoice = findViewById(R.id.ambientGlowChoice);
        ambientGlowChoice.setOnClickListener(v -> {
            ambientGlow = !ambientGlow;
            choices.edit().putBoolean("ambient_glow", ambientGlow).apply();
            changed();
            refresh();
        });
        dockGlassChoice = findViewById(R.id.dockGlassChoice);
        dockGlassChoice.setOnClickListener(v -> {
            dockGlass = !dockGlass;
            choices.edit().putBoolean("dock_glass", dockGlass).apply();
            changed();
            refresh();
        });
        cinematicChoice = findViewById(R.id.cinematicChoice);
        cinematicChoice.setOnClickListener(v -> {
            cinematicHome = !cinematicHome;
            choices.edit().putBoolean("cinematic_home", cinematicHome).apply();
            changed();
            refresh();
        });
        showStatsChoice = findViewById(R.id.showStatsChoice);
        showStatsChoice.setOnClickListener(v -> {
            showStats = !showStats;
            choices.edit().putBoolean("show_stats", showStats).apply();
            changed();
            refresh();
        });
        showBluetoothChoice = findViewById(R.id.showBluetoothChoice);
        showBluetoothChoice.setOnClickListener(v -> {
            showBluetoothPill = !showBluetoothPill;
            choices.edit().putBoolean("show_bluetooth_pill", showBluetoothPill).apply();
            changed();
            refresh();
        });
        showInputsChoice = findViewById(R.id.showInputsChoice);
        showInputsChoice.setOnClickListener(v -> {
            showInputsPill = !showInputsPill;
            choices.edit().putBoolean("show_inputs_pill", showInputsPill).apply();
            changed();
            refresh();
        });
        showNetworkChoice = findViewById(R.id.showNetworkChoice);
        showNetworkChoice.setOnClickListener(v -> {
            showNetworkPill = !showNetworkPill;
            choices.edit().putBoolean("show_network_pill", showNetworkPill).apply();
            changed();
            refresh();
        });
        wifiStatusColorChoice = findViewById(R.id.wifiStatusColorChoice);
        wifiStatusColorChoice.setOnClickListener(v -> {
            wifiStatusColor = !wifiStatusColor;
            choices.edit().putBoolean("wifi_status_color", wifiStatusColor).apply();
            changed();
            refresh();
        });
        iconOnlyButtonsChoice = findViewById(R.id.iconOnlyButtonsChoice);
        iconOnlyButtonsChoice.setOnClickListener(v -> {
            iconOnlyButtons = !iconOnlyButtons;
            choices.edit().putBoolean("icon_only_buttons", iconOnlyButtons).apply();
            changed();
            refresh();
        });
        fadeRowsChoice = findViewById(R.id.fadeRowsChoice);
        fadeRowsChoice.setOnClickListener(v -> {
            fadeRows = !fadeRows;
            choices.edit().putBoolean("fade_rows", fadeRows).apply();
            changed();
            refresh();
        });
        settingsKeyPanelChoice = findViewById(R.id.settingsKeyPanelChoice);
        settingsKeyPanelChoice.setOnClickListener(v -> {
            settingsKeyPanel = !settingsKeyPanel;
            choices.edit().putBoolean("settings_key_panel", settingsKeyPanel).apply();
            changed();
            refresh();
        });
        festivalChoice = findViewById(R.id.festivalChoice);
        festivalChoice.setOnClickListener(v -> {
            festivalDiwali = !festivalDiwali;
            choices.edit().putString("festival", festivalDiwali ? "diwali" : "none").apply();
            changed();
            refresh();
        });
        tileMenuChoice = findViewById(R.id.tileMenuChoice);
        tileMenuChoice.setOnClickListener(v -> {
            tileMenu = !tileMenu;
            choices.edit().putBoolean("tile_menu", tileMenu).apply();
            changed();
            refresh();
        });
        screensaverChoice = findViewById(R.id.screensaverChoice);
        screensaverChoice.setOnClickListener(v -> showScreensaverPicker());
        remapNetflixChoice = findViewById(R.id.remapNetflixChoice);
        remapNetflixChoice.setOnClickListener(v -> showAppPicker("remap_netflix", R.string.remap_netflix_label));
        remapPrimeChoice = findViewById(R.id.remapPrimeChoice);
        remapPrimeChoice.setOnClickListener(v -> showAppPicker("remap_prime", R.string.remap_prime_label));
        remapHotstarChoice = findViewById(R.id.remapHotstarChoice);
        remapHotstarChoice.setOnClickListener(v -> showAppPicker("remap_hotstar", R.string.remap_hotstar_label));
        remapSonyLivChoice = findViewById(R.id.remapSonyLivChoice);
        remapSonyLivChoice.setOnClickListener(v -> showAppPicker("remap_sonyliv", R.string.remap_sonyliv_label));
        remapLiveTvChoice = findViewById(R.id.remapLiveTvChoice);
        remapLiveTvChoice.setOnClickListener(v -> showAppPicker("remap_livetv", R.string.remap_livetv_label));
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY)
                    .remove("ambient_glow").remove("dock_glass")
                    .remove("show_stats").remove("show_bluetooth_pill")
                    .remove("show_inputs_pill").remove("show_network_pill")
                    .remove("wifi_status_color").remove("icon_only_buttons")
                    .remove("cinematic_home").remove("fade_rows")
                    .remove("settings_key_panel").remove("festival").remove("tile_menu")
                    .remove("home_idle_clock_minutes")
                    .remove("remap_netflix").remove("remap_prime")
                    .remove("remap_hotstar").remove("remap_sonyliv").remove("remap_livetv").commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            ambientGlow = current.ambientGlow;
            dockGlass = current.dockGlass;
            showStats = current.showStats;
            showBluetoothPill = current.showBluetoothPill;
            showInputsPill = current.showInputsPill;
            showNetworkPill = current.showNetworkPill;
            wifiStatusColor = current.wifiStatusColor;
            iconOnlyButtons = current.iconOnlyButtons;
            cinematicHome = current.cinematicHome;
            fadeRows = current.fadeRows;
            settingsKeyPanel = current.settingsKeyPanel;
            festivalDiwali = current.festival != null && "diwali".equalsIgnoreCase(current.festival.name);
            tileMenu = current.tileMenu;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                        new File(getFilesDir(), Config.WALLPAPER_NAME).delete();
                    } else {
                        File picked = images.get(which);
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                picked.getName()).apply();
                        Config.syncWallpaperCopy(picked, new File(getFilesDir(), Config.WALLPAPER_NAME));
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    new File(getFilesDir(), Config.WALLPAPER_NAME).delete();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static final int[] SCREENSAVER_TIMEOUTS = {0, 5, 10, 15, 30};

    private void showScreensaverPicker() {
        String[] labels = new String[SCREENSAVER_TIMEOUTS.length];
        for (int i = 0; i < SCREENSAVER_TIMEOUTS.length; i++) {
            int mins = SCREENSAVER_TIMEOUTS[i];
            labels[i] = mins == 0 ? getString(R.string.screensaver_off)
                    : getString(R.string.screensaver_minutes, mins);
        }
        int current = choices.getInt("home_idle_clock_minutes", 10);
        int checked = 2;
        for (int i = 0; i < SCREENSAVER_TIMEOUTS.length; i++) {
            if (SCREENSAVER_TIMEOUTS[i] == current) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.screensaver_dialog_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    int chosenMins = SCREENSAVER_TIMEOUTS[which];
                    choices.edit().putInt("home_idle_clock_minutes", chosenMins).apply();
                    changed();
                    refresh();
                })
                .setPositiveButton(R.string.test_screensaver_now, (dialog, which) -> {
                    startActivity(new Intent(this, ClockActivity.class));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class AppEntry implements Comparable<AppEntry> {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
        @Override
        public int compareTo(AppEntry other) {
            return this.label.compareToIgnoreCase(other.label);
        }
    }

    private void showAppPicker(String prefKey, int titleRes) {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        if (list.isEmpty()) {
            main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            list = pm.queryIntentActivities(main, 0);
        }
        Map<String, String> appMap = new HashMap<>();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                String pkg = ri.activityInfo.packageName;
                if (!appMap.containsKey(pkg)) {
                    CharSequence label = ri.loadLabel(pm);
                    appMap.put(pkg, label != null ? label.toString() : pkg);
                }
            }
        }
        List<AppEntry> apps = new ArrayList<>();
        for (Map.Entry<String, String> entry : appMap.entrySet()) {
            apps.add(new AppEntry(entry.getValue(), entry.getKey()));
        }
        Collections.sort(apps);

        String currentPkg = choices.getString(prefKey, null);
        String[] items = new String[apps.size() + 2];
        items[0] = getString(R.string.remap_default);
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            AppEntry app = apps.get(i);
            items[i + 1] = app.label;
            if (app.packageName.equals(currentPkg)) {
                checked = i + 1;
            }
        }
        items[items.length - 1] = getString(R.string.remap_none);
        if ("none".equals(currentPkg)) {
            checked = items.length - 1;
        }

        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        choices.edit().remove(prefKey).apply();
                        Toast.makeText(this, getString(R.string.remap_applied, getString(titleRes), getString(R.string.remap_default)), Toast.LENGTH_SHORT).show();
                    } else if (which == items.length - 1) {
                        choices.edit().putString(prefKey, "none").apply();
                        Toast.makeText(this, getString(R.string.remap_applied, getString(titleRes), getString(R.string.remap_none)), Toast.LENGTH_SHORT).show();
                    } else {
                        AppEntry chosen = apps.get(which - 1);
                        choices.edit().putString(prefKey, chosen.packageName).apply();
                        Toast.makeText(this, getString(R.string.remap_applied, getString(titleRes), chosen.label), Toast.LENGTH_SHORT).show();
                    }
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String getAppLabel(String pkg) {
        if (pkg == null) return getString(R.string.remap_default);
        if ("none".equals(pkg) || pkg.isEmpty()) return getString(R.string.remap_none);
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            return label != null ? label.toString() : pkg;
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        ambientGlowChoice.setText(getString(R.string.ambient_glow_state,
                getString(ambientGlow ? R.string.on : R.string.off)));
        dockGlassChoice.setText(getString(R.string.dock_glass_state,
                getString(dockGlass ? R.string.on : R.string.off)));
        cinematicChoice.setText(getString(R.string.cinematic_home_state,
                getString(cinematicHome ? R.string.on : R.string.off)));
        showStatsChoice.setText(getString(R.string.show_stats_state,
                getString(showStats ? R.string.on : R.string.off)));
        showBluetoothChoice.setText(getString(R.string.show_bluetooth_pill_state,
                getString(showBluetoothPill ? R.string.on : R.string.off)));
        showInputsChoice.setText(getString(R.string.show_inputs_pill_state,
                getString(showInputsPill ? R.string.on : R.string.off)));
        showNetworkChoice.setText(getString(R.string.show_network_pill_state,
                getString(showNetworkPill ? R.string.on : R.string.off)));
        wifiStatusColorChoice.setText(getString(R.string.wifi_status_color_state,
                getString(wifiStatusColor ? R.string.on : R.string.off)));
        iconOnlyButtonsChoice.setText(getString(R.string.icon_only_buttons_state,
                getString(iconOnlyButtons ? R.string.on : R.string.off)));
        fadeRowsChoice.setText(getString(R.string.fade_rows_state,
                getString(fadeRows ? R.string.on : R.string.off)));
        settingsKeyPanelChoice.setText(getString(R.string.settings_key_panel_state,
                getString(settingsKeyPanel ? R.string.settings_key_panel_quick : R.string.settings_key_panel_system)));
        festivalChoice.setText(getString(R.string.festival_skin_state,
                getString(festivalDiwali ? R.string.on : R.string.off)));
        tileMenuChoice.setText(getString(R.string.tile_menu_state,
                getString(tileMenu ? R.string.on : R.string.off)));
        int idleMins = choices.getInt("home_idle_clock_minutes", 10);
        screensaverChoice.setText(getString(R.string.screensaver_choice_label,
                idleMins == 0 ? getString(R.string.screensaver_off) : getString(R.string.screensaver_minutes, idleMins)));
        remapNetflixChoice.setText(getString(R.string.remap_netflix_format,
                getAppLabel(choices.getString("remap_netflix", null))));
        remapPrimeChoice.setText(getString(R.string.remap_prime_format,
                getAppLabel(choices.getString("remap_prime", null))));
        remapHotstarChoice.setText(getString(R.string.remap_hotstar_format,
                getAppLabel(choices.getString("remap_hotstar", null))));
        remapSonyLivChoice.setText(getString(R.string.remap_sonyliv_format,
                getAppLabel(choices.getString("remap_sonyliv", null))));
        remapLiveTvChoice.setText(getString(R.string.remap_livetv_format,
                getAppLabel(choices.getString("remap_livetv", null))));
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains("ambient_glow") || choices.contains("dock_glass")
                || choices.contains("show_stats") || choices.contains("show_bluetooth_pill")
                || choices.contains("show_inputs_pill") || choices.contains("show_network_pill")
                || choices.contains("wifi_status_color") || choices.contains("icon_only_buttons")
                || choices.contains("cinematic_home") || choices.contains("fade_rows")
                || choices.contains("settings_key_panel") || choices.contains("festival")
                || choices.contains("tile_menu") || choices.contains("home_idle_clock_minutes")
                || choices.contains("remap_netflix") || choices.contains("remap_prime")
                || choices.contains("remap_hotstar") || choices.contains("remap_sonyliv")
                || choices.contains("remap_livetv");
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
