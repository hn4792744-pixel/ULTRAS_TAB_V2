package me.uc.hussein.ultrastab.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

@Plugin(
        id = "ultrastab",
        name = "ULTRAS_TAB_V1",
        version = "v1",
        description = "ULTRAS TAB Velocity",
        authors = {"Hussein"}
)
public final class UltrasTabVelocity {

    private final Logger logger;
    private final ProxyServer proxyServer;

    private VelocityTabConfig config;
    private VelocityTabRanks ranks;
    private VelocityTabManager tabManager;

    @Inject
    public UltrasTabVelocity(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try (var input = getClass().getClassLoader().getResourceAsStream("tab.yml")) {
            config = VelocityTabConfig.load(input);
            ranks = new VelocityTabRanks(config);
            tabManager = new VelocityTabManager(proxyServer, config, ranks);

            logger.info("ULTRAS_TAB_V1 Velocity plugin enabled.");
            logger.info("TAB enabled: {}", config.isEnabled());
            logger.info("TAB groups: {}", ranks.getRanks().size());
            logger.info("TAB profiles: {}", config.getProfiles().size());

            for (VelocityTabRank rank : ranks.getRanks()) {
                logger.info(
                        "TAB rank: {} | EN={} | AR={} | color={} | permission={}",
                        rank.key(),
                        rank.displayEn(),
                        rank.displayAr(),
                        rank.color(),
                        rank.permission()
                );
            }

            proxyServer.getScheduler()
                    .buildTask(this, this::updateAllPlayers)
                    .repeat(1, TimeUnit.SECONDS)
                    .schedule();

        } catch (Exception e) {
            logger.error("Failed to load tab.yml.", e);
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (tabManager != null) {
            tabManager.update(event.getPlayer());
        }
    }

    private void updateAllPlayers() {
        if (tabManager == null) {
            return;
        }

        for (var player : proxyServer.getAllPlayers()) {
            tabManager.update(player);
        }
    }
}
