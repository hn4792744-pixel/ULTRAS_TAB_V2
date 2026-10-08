package me.uc.hussein.ultrastab;

import me.uc.hussein.ultrastab.animation.AnimationManager;
import me.uc.hussein.ultrastab.command.TabAdminCommand;
import me.uc.hussein.ultrastab.command.TabCommand;
import me.uc.hussein.ultrastab.gui.SettingsGui;
import me.uc.hussein.ultrastab.integration.PlaceholderManager;
import me.uc.hussein.ultrastab.language.LanguageManager;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.listener.PlayerListener;
import me.uc.hussein.ultrastab.nametag.NametagManager;
import me.uc.hussein.ultrastab.scoreboard.ScoreboardManager;
import me.uc.hussein.ultrastab.settings.SettingsManager;
import me.uc.hussein.ultrastab.tab.TabManager;
import me.uc.hussein.ultrastab.util.Sounds;
import me.uc.hussein.ultrastab.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

/**
 * ULTRAS_TAB_V1 - tab list, scoreboard, nametags and animations.
 * <p>
 * Everything visual is driven by ONE repeating task ({@link #tick()}): it advances the shared animation
 * clock and asks each manager to update only the players whose output actually changed.
 */
public final class UltrasTabPlugin extends JavaPlugin {

    private static final String[] RESOURCES = {
            "tab.yml", "scoreboard.yml", "nametag.yml", "animations.yml",
            "messages/en.yml", "messages/ar.yml",
            "gui/settings.yml", "gui/tab.yml", "gui/scoreboard.yml", "gui/nametag.yml", "gui/privacy.yml"
    };
    private static final long TICK_PERIOD = 2L;
    private static final int FLUSH_EVERY_TICKS = 100;

    private SettingsManager settings;
    private LanguageManager lang;
    private AnimationManager animations;
    private PlaceholderManager placeholders;
    private TabManager tab;
    private ScoreboardManager scoreboard;
    private NametagManager nametag;
    private SettingsGui gui;
    private Sounds sounds;

    private BukkitTask task;
    private PlayerLanguage defaultLanguage = PlayerLanguage.EN;
    private long updateInterval = 1000L;
    private boolean debug;
    private int flushCounter;
    private long lastErrorLog;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getConfig().getBoolean("enabled", true)) {
            getLogger().info("Disabled in config.yml (enabled: false).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        for (String resource : RESOURCES) {
            if (!new File(getDataFolder(), resource).exists()) {
                saveResource(resource, false);
            }
        }

        settings = new SettingsManager(this);
        lang = new LanguageManager(this);
        animations = new AnimationManager();
        placeholders = new PlaceholderManager(this);
        sounds = new Sounds(this);
        tab = new TabManager(this);
        scoreboard = new ScoreboardManager(this);
        nametag = new NametagManager(this);
        gui = new SettingsGui(this);

        loadAll();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(gui, this);

        PluginCommand command = getCommand("tab");
        if (command != null) {
            TabCommand executor = new TabCommand(this, new TabAdminCommand(this));
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
        }
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, TICK_PERIOD);
        getLogger().info("ULTRAS_TAB_V1 enabled.");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
        }
        if (tab != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tab.clear(player);
                scoreboard.release(player);
            }
            gui.closeAll();
        }
        if (settings != null) {
            settings.saveAllNow();
        }
    }

    // ------------------------------------------------------------ loading

    private YamlConfiguration yaml(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

    private void loadAll() {
        reloadConfig();
        updateInterval = Math.max(250L, getConfig().getLong("update-interval", 1000L));
        debug = getConfig().getBoolean("debug", false);
        defaultLanguage = PlayerLanguage.from(getConfig().getString("default-language", "en"), PlayerLanguage.EN);

        Text.clearCache();
        sounds.load(getConfig());
        placeholders.load(getConfig());
        lang.load();
        animations.load(yaml("animations.yml"));
        tab.load(yaml("tab.yml"));
        scoreboard.load(yaml("scoreboard.yml"));
        nametag.load(yaml("nametag.yml"));
        gui.load();
    }

    /** Reloads every config file and refreshes every online player, without a restart. */
    public void reloadAll() {
        gui.closeAll();
        loadAll();
    }

    // ------------------------------------------------------------ players

    public void addPlayer(Player player) {
        settings.get(player);
        scoreboard.attach(player);
        tab.join(player);
        nametag.join(player);
    }

    public void removePlayer(Player player) {
        nametag.remove(player);
        tab.quit(player);
        scoreboard.detach(player);
        settings.unload(player.getUniqueId());
    }

    /** Called after any personal setting changed so only the affected systems refresh. */
    public void applyChange(Player player, String key) {
        switch (key) {
            case "tab", "tab-rank" -> tab.force(player);
            case "scoreboard", "scoreboard-field", "compact" -> scoreboard.force(player);
            case "animations" -> {
                tab.force(player);
                scoreboard.force(player);
            }
            case "nametag-rank" -> nametag.refreshTarget(player);
            case "nametag-ranks", "nametag-name" -> nametag.refreshViewer(player);
            case "language" -> {
                tab.force(player);
                scoreboard.force(player);
                nametag.refreshViewer(player);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------ shared tick

    private void tick() {
        try {
            long now = System.currentTimeMillis();
            Set<String> changed = animations.advance(now);
            tab.tick(now, changed);
            scoreboard.tick(now, changed);
            if (++flushCounter >= FLUSH_EVERY_TICKS) {
                flushCounter = 0;
                settings.flush();
            }
        } catch (RuntimeException ex) {
            long now = System.currentTimeMillis();
            if (debug || now - lastErrorLog > 30_000L) {
                lastErrorLog = now;
                getLogger().log(Level.WARNING, "Update tick failed (will retry)", ex);
            }
        }
    }

    // ------------------------------------------------------------ accessors

    public SettingsManager settings() {
        return settings;
    }

    public LanguageManager lang() {
        return lang;
    }

    public AnimationManager animations() {
        return animations;
    }

    public PlaceholderManager placeholders() {
        return placeholders;
    }

    public TabManager tab() {
        return tab;
    }

    public ScoreboardManager scoreboard() {
        return scoreboard;
    }

    public NametagManager nametag() {
        return nametag;
    }

    public SettingsGui gui() {
        return gui;
    }

    public Sounds sounds() {
        return sounds;
    }

    public PlayerLanguage defaultLanguage() {
        return defaultLanguage;
    }

    public long updateInterval() {
        return updateInterval;
    }

    public boolean debug() {
        return debug;
    }
}
