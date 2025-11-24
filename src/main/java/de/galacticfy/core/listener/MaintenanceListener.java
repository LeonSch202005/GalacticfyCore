package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.MaintenanceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.server.ServerPing;

public class MaintenanceListener {

    private final MaintenanceService maintenanceService;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MaintenanceListener(MaintenanceService maintenanceService, Logger logger) {
        this.maintenanceService = maintenanceService;
        this.logger = logger;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!maintenanceService.isMaintenanceEnabled()) return;

        Player player = event.getPlayer();

        // Bypass-Permission
        if (player.hasPermission("galacticfy.maintenance.bypass")) {
            return;
        }

        // Spieler kicken bevor er verbindet
        event.setResult(ResultedEvent.ComponentResult.denied(
                Component.text(maintenanceService.getMessage())
        ));
        logger.info("Spieler {} wurde wegen Maintenance geblockt.", player.getUsername());
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        if (!maintenanceService.isMaintenanceEnabled()) return;

        ServerPing original = event.getPing();
        ServerPing.Builder builder = original.asBuilder();

        // MOTD im Ping ändern
        builder.description(
                miniMessage.deserialize("<red><bold>Wartungsarbeiten</bold></red><gray> - Schau später wieder vorbei!</gray>")
        );

        event.setPing(builder.build());
    }
}
