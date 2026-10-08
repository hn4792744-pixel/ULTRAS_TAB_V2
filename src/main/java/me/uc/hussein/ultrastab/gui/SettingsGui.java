package me.uc.hussein.ultrastab.gui;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import me.uc.hussein.ultrastab.language.PlayerLanguage;
import me.uc.hussein.ultrastab.settings.PlayerSettings;
import me.uc.hussein.ultrastab.util.Cfg;
import me.uc.hussein.ultrastab.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The personal settings menus. One generic implementation drives every page; each page is a file in gui/
 * (settings, tab, scoreboard, nametag, privacy) whose items declare a "type":
 * open (submenu), toggle (a boolean setting), field (a scoreboard line), compact, language or close.
 */
public final class SettingsGui implements Listener {

    private static final String[] MENUS = {"settings", "tab", "scoreboard", "nametag", "privacy"};

    private static final class Holder implements InventoryHolder {
        final String menu;
        Inventory inventory;

        Holder(String menu) {
            this.menu = menu;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final UltrasTabPlugin plugin;
    private final Map<String, YamlConfiguration> menus = new HashMap<>();

    public SettingsGui(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        menus.clear();
        for (String name : MENUS) {
            File file = new File(plugin.getDataFolder(), "gui/" + name + ".yml");
            if (file.isFile()) {
                menus.put(name, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public void open(Player player, String menu) {
        YamlConfiguration yaml = menus.get(menu);
        if (yaml == null) {
            return;
        }
        PlayerLanguage lang = plugin.settings().get(player).language();
        Holder holder = new Holder(menu);
        int size = Math.max(9, Math.min(54, (yaml.getInt("size", 27) / 9) * 9));
        holder.inventory = Bukkit.createInventory(holder, size, Text.parse(Cfg.text(yaml, "title", lang)));
        render(holder, player);
        player.openInventory(holder.inventory);
        plugin.sounds().play(player, "open");
    }

    /** Closes every open settings menu (used on reload/disable so stale layouts are never clickable). */
    public void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Holder) {
                p.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------ rendering

    private void render(Holder holder, Player player) {
        YamlConfiguration yaml = menus.get(holder.menu);
        ConfigurationSection items = yaml.getConfigurationSection("items");
        PlayerSettings ps = plugin.settings().get(player);
        PlayerLanguage lang = ps.language();
        holder.inventory.clear();
        if (plugin.getConfig().getBoolean("gui.filler", false)) {
            ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta paneMeta = pane.getItemMeta();
            paneMeta.displayName(Component.space());
            pane.setItemMeta(paneMeta);
            for (int i = 0; i < holder.inventory.getSize(); i++) {
                holder.inventory.setItem(i, pane);
            }
        }
        if (items == null) {
            return;
        }
        for (String id : items.getKeys(false)) {
            ConfigurationSection it = items.getConfigurationSection(id);
            if (it == null) {
                continue;
            }
            int slot = it.getInt("slot", -1);
            if (slot < 0 || slot >= holder.inventory.getSize()) {
                continue;
            }
            Boolean on = stateOf(it, ps);
            holder.inventory.setItem(slot, build(it, ps, lang, on));
        }
    }

    /** ON/OFF for toggle-like items, null for items without a state. */
    private Boolean stateOf(ConfigurationSection it, PlayerSettings ps) {
        return switch (it.getString("type", "open")) {
            case "toggle" -> ps.flag(it.getString("setting", ""));
            case "field" -> !ps.hidden(it.getString("field", ""));
            default -> null;
        };
    }

    private ItemStack build(ConfigurationSection it, PlayerSettings ps, PlayerLanguage lang, Boolean on) {
        Material material = Material.matchMaterial(it.getString("material", "PAPER"));
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();

        Map<String, String> ph = new HashMap<>();
        ph.put("state", plugin.lang().raw(lang, on != null && on ? "gui.on" : "gui.off"));
        ph.put("mode", plugin.lang().raw(lang, "compact.mode." + ps.compact()));
        ph.put("language", plugin.lang().raw(lang, "language.name." + lang.code()));
        ph.put("c", on == null ? "#8aa4d6" : on ? "#7fc29b" : "#d96c6c");
        ph.put("sym", on == null ? "•" : on ? "✓" : "×");

        meta.displayName(styled(Cfg.text(it, "name", lang), ph));
        List<Component> lore = new ArrayList<>();
        for (String line : Cfg.lines(it, "lore", lang)) {
            lore.add(styled(line, ph));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        meta.addItemFlags(ItemFlag.values());
        if (on != null && on) {
            meta.setEnchantmentGlintOverride(true);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private Component styled(String template, Map<String, String> ph) {
        String text = template;
        for (Map.Entry<String, String> e : ph.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }
        return Text.parse(text).decoration(TextDecoration.ITALIC, false);
    }

    // ------------------------------------------------------------ clicks

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        YamlConfiguration yaml = menus.get(holder.menu);
        ConfigurationSection items = yaml == null ? null : yaml.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String id : items.getKeys(false)) {
            ConfigurationSection it = items.getConfigurationSection(id);
            if (it != null && it.getInt("slot", -1) == event.getSlot()) {
                handle(player, holder, it);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private void handle(Player player, Holder holder, ConfigurationSection it) {
        switch (it.getString("type", "open")) {
            case "open" -> {
                plugin.sounds().play(player, "click");
                open(player, it.getString("menu", "settings"));
            }
            case "close" -> {
                plugin.sounds().play(player, "click");
                player.closeInventory();
            }
            case "toggle" -> {
                plugin.settings().toggle(player, it.getString("setting", ""));
                render(holder, player);
            }
            case "field" -> {
                plugin.settings().toggleField(player, it.getString("field", ""));
                render(holder, player);
            }
            case "compact" -> {
                plugin.settings().cycleCompact(player);
                render(holder, player);
            }
            case "language" -> {
                plugin.settings().setLanguage(player, plugin.settings().get(player).language().other());
                open(player, holder.menu); // the title changes with the language, so reopen
            }
            default -> {
            }
        }
    }
}
