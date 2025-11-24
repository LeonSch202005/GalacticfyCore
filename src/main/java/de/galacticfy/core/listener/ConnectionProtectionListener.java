package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Set;

public class ConnectionProtectionListener {

    private final Logger logger;

    // Beispiel: Dev-/Event-Server dürfen nur mit Permission betreten werden
    private final Set<String> restrictedServers = Set.of(
            "dev-1",
            "event-1"
    );

    public ConnectionProtectionListener(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();

        if (event.getOriginalServer() == null) {
            return;
        }

        String target = event.getOriginalServer().getServerInfo().getName();

        // Event-Server nur mit Permission galacticfy.event.join
        if (target.equalsIgnoreCase("event-1") && !player.hasPermission("galacticfy.event.join")) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.text("§cDu darfst diesem Event nicht beitreten."));
            logger.info("Spieler {} wurde daran gehindert, {} ohne Permission zu joinen.", player.getUsername(), target);
            return;
        }

        // Generelle Restricted-Server-Blacklist (nur mit bypass)
        if (restrictedServers.contains(target.toLowerCase())
                && !player.hasPermission("galacticfy.bypass.serverblacklist")) {

            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.text("§cDu kannst diesen Server nicht direkt betreten."));
            logger.info("Spieler {} wurde daran gehindert, restricted Server {} zu joinen.", player.getUsername(), target);
        }
    }
}
