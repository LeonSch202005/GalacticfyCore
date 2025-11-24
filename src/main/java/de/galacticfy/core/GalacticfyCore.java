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
import de.galacticfy.core.command.SendCommand;
import de.galacticfy.core.command.SkyblockCommand;
import de.galacticfy.core.listener.ConnectionProtectionListener;
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

    @Inject
    public GalacticfyCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.teleportService = new ServerTeleportService(proxy, logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GalacticfyCore wurde initialisiert.");

        CommandManager commandManager = proxy.getCommandManager();

        // /hub /lobby /spawn
        CommandMeta hubMeta = commandManager.metaBuilder("hub")
                .aliases("lobby", "spawn")
                .build();
        commandManager.register(hubMeta, new HubCommand(teleportService));

        // /citybuild /cb
        CommandMeta cbMeta = commandManager.metaBuilder("citybuild")
                .aliases("cb")
                .build();
        commandManager.register(cbMeta, new CitybuildCommand(teleportService));

        // /skyblock /sb
        CommandMeta sbMeta = commandManager.metaBuilder("skyblock")
                .aliases("sb")
                .build();
        commandManager.register(sbMeta, new SkyblockCommand(teleportService));

        // /event
        CommandMeta eventMeta = commandManager.metaBuilder("event")
                .build();
        commandManager.register(eventMeta, new EventCommand(teleportService));

        // /send <Spieler> <Server> (Staff)
        CommandMeta sendMeta = commandManager.metaBuilder("send")
                .build();
        commandManager.register(sendMeta, new SendCommand(proxy, teleportService));

        // Listener für Sicherheitsfeatures
        proxy.getEventManager().register(this, new ConnectionProtectionListener(logger));

        logger.info("GalacticfyCore: Commands & Listener registriert.");
    }
}
