package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.galacticfy.core.service.MaintenanceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

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

        event.setResult(ResultedEvent.ComponentResult.denied(
                Component.text("§cDas Netzwerk befindet sich zurzeit in Wartungsarbeiten.")
        ));
        logger.info("Spieler {} wurde wegen Maintenance geblockt.", player.getUsername());
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing original = event.getPing();
        ServerPing.Builder builder = original.asBuilder();

        if (maintenanceService.isMaintenanceEnabled()) {
            // Wartungs-MOTD
            Long remaining = maintenanceService.getRemainingMillis();

            String extra = "";
            if (remaining != null && remaining > 0) {
                long seconds = remaining / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                long s = seconds % 60;
                long m = minutes % 60;
                long h = hours % 24;

                extra = "<gray>Restzeit: </gray><yellow>"
                        + days + "d " + h + "h " + m + "m " + s + "s</yellow>";
            }

            builder.description(
                    miniMessage.deserialize(
                            "<red><bold>WARTUNGSARBEITEN</bold></red><newline>" +
                                    "<gray>Schau später wieder vorbei!</gray>" +
                                    (extra.isEmpty() ? "" : "<newline>" + extra)
                    )
            );
        } else {
            // Normale, hübsche MOTD
            builder.description(
                    miniMessage.deserialize(
                            "<gradient:#00AEEF:#007FFF><bold>Galacticfy</bold></gradient> <gray>- Willkommen</gray>"
                    )
            );
        }

        event.setPing(builder.build());
    }
}
