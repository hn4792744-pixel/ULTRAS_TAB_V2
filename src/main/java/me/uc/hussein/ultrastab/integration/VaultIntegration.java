package me.uc.hussein.ultrastab.integration;

import me.uc.hussein.ultrastab.UltrasTabPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Optional Vault economy bridge (reflection, no build dependency). {@link #balance(OfflinePlayer)}
 * returns null when Vault or an economy provider is unavailable; the caller shows a fallback.
 */
public final class VaultIntegration {

    private final UltrasTabPlugin plugin;
    private boolean enabled = true;
    private Object economy;
    private Method getBalance;
    private Method format;
    private long nextAttempt;

    public VaultIntegration(UltrasTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(boolean enabled) {
        this.enabled = enabled;
        this.economy = null;
        this.nextAttempt = 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean resolve() {
        if (economy != null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (!enabled || now < nextAttempt || !Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }
        nextAttempt = now + 30_000L;
        try {
            Class economyClass = Class.forName("net.milkbowl.vault.economy.Economy", true, plugin.getClass().getClassLoader());
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider == null) {
                return false;
            }
            getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            format = economyClass.getMethod("format", double.class);
            economy = provider.getProvider();
            return true;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    public String balance(OfflinePlayer player) {
        if (!resolve()) {
            return null;
        }
        try {
            double amount = ((Number) getBalance.invoke(economy, player)).doubleValue();
            return String.valueOf(format.invoke(economy, amount));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            economy = null;
            return null;
        }
    }
}
