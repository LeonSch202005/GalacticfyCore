package de.galacticfy.core;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.command.CitybuildCommand;
import de.galacticfy.core.command.EventCommand;
import de.galacticfy.core.command.HubCommand;
import de.galacticfy.core.command.MaintenanceCommand;
import de.galacticfy.core.command.SendCommand;
import de.galacticfy.core.command.SkyblockCommand;
import de.galacticfy.core.listener.ConnectionProtectionListener;
import de.galacticfy.core.listener.MaintenanceListener;
import de.galacticfy.core.service.MaintenanceService;
import de.galacticfy.core.service.ServerTeleportService;
import org.slf4j.Logger;

@Plugin(
        id = "galacticfycore",
        name = "GalacticfyCore",
        version = "0.0.0",
        url = "https://galacticfy.de"
)
public class GalacticfyCore {

    private final ProxyServer proxy;
    private final Logger logger;
    private final ServerTeleportService teleportService;
    private final MaintenanceService maintenanceService;

    @Inject
    public GalacticfyCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.teleportService = new ServerTeleportService(proxy, logger);
        this.maintenanceService = new MaintenanceService(logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GalacticfyCore wurde initialisiert.");

        CommandManager commandManager = proxy.getCommandManager();

        // /hub /lobby /spawn -> Lobby-1
        CommandMeta hubMeta = commandManager.metaBuilder("hub")
                .aliases("lobby", "spawn")
                .build();
        commandManager.register(hubMeta, new HubCommand(teleportService));

        // /citybuild /cb -> Citybuild-1
        CommandMeta cbMeta = commandManager.metaBuilder("citybuild")
                .aliases("cb")
                .build();
        commandManager.register(cbMeta, new CitybuildCommand(teleportService));

        // /skyblock /sb -> skyblock-core-1
        CommandMeta sbMeta = commandManager.metaBuilder("skyblock")
                .aliases("sb")
                .build();
        commandManager.register(sbMeta, new SkyblockCommand(teleportService));

        // /event -> Event-Server (nur mit Permission)
        CommandMeta eventMeta = commandManager.metaBuilder("event")
                .build();
        commandManager.register(eventMeta, new EventCommand(teleportService));

        // /send <Spieler> <Server> (Staff-Command)
        CommandMeta sendMeta = commandManager.metaBuilder("send")
                .build();
        commandManager.register(sendMeta, new SendCommand(proxy, teleportService));

        // /maintenance (globaler Wartungsmodus)
        CommandMeta maintMeta = commandManager.metaBuilder("maintenance")
                .aliases("maint")
                .build();
        commandManager.register(maintMeta, new MaintenanceCommand(maintenanceService, proxy));


        // Listener registrieren
        proxy.getEventManager().register(this, new ConnectionProtectionListener(logger));
        proxy.getEventManager().register(this, new MaintenanceListener(maintenanceService, logger));

        logger.info("GalacticfyCore: Commands & Listener registriert.");
    }
}
