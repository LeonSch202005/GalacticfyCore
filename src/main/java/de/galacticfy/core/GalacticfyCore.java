package de.galacticfy.core;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
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
import de.galacticfy.core.motd.GalacticfyMotdProvider;
import de.galacticfy.core.service.MaintenanceService;
import de.galacticfy.core.service.ServerTeleportService;
import de.galacticfy.core.util.DiscordWebhookNotifier;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

@Plugin(
        id = "galacticfycore",
        name = "GalacticfyCore",
        version = "0.0.0",
        url = "https://galacticfy.de",
        dependencies = {
                @Dependency(id = "luckperms")
        }
)
public class GalacticfyCore {

    private final ProxyServer proxy;
    private final Logger logger;
    private final ServerTeleportService teleportService;
    private final MaintenanceService maintenanceService;

    private DiscordWebhookNotifier discordNotifier;

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

        LuckPerms luckPerms = LuckPermsProvider.get();

        String webhookUrl = "https://discord.com/api/webhooks/1441711193021747274/b7v7Q67KYqFZCrubk8T6-0KASMLzaf50wHA_jVDCyIA5qEhuJZVuXP_Le42XiUSliSz_";
        this.discordNotifier = new DiscordWebhookNotifier(logger, webhookUrl);

        CommandManager commandManager = proxy.getCommandManager();

        // /hub /lobby /spawn
        CommandMeta hubMeta = commandManager.metaBuilder("hub")
                .aliases("lobby", "spawn")
                .build();
        commandManager.register(hubMeta, new HubCommand(teleportService, maintenanceService));

        // /citybuild /cb
        CommandMeta cbMeta = commandManager.metaBuilder("citybuild")
                .aliases("cb")
                .build();
        commandManager.register(cbMeta, new CitybuildCommand(teleportService, maintenanceService));

        // /skyblock /sb
        CommandMeta sbMeta = commandManager.metaBuilder("skyblock")
                .aliases("sb")
                .build();
        commandManager.register(sbMeta, new SkyblockCommand(teleportService, maintenanceService));

        // /event
        CommandMeta eventMeta = commandManager.metaBuilder("event")
                .build();
        commandManager.register(eventMeta, new EventCommand(teleportService, maintenanceService));

        // /send
        CommandMeta sendMeta = commandManager.metaBuilder("send")
                .build();
        commandManager.register(sendMeta, new SendCommand(proxy, teleportService));

        // /maintenance (TECH)
        CommandMeta maintenanceMeta = commandManager.metaBuilder("maintenance")
                .build();
        commandManager.register(
                maintenanceMeta,
                new MaintenanceCommand(maintenanceService, proxy, luckPerms, discordNotifier, false)
        );

        // /wartung (DE)
        CommandMeta wartungMeta = commandManager.metaBuilder("wartung")
                .build();
        commandManager.register(
                wartungMeta,
                new MaintenanceCommand(maintenanceService, proxy, luckPerms, discordNotifier, true)
        );

        // Listener
        proxy.getEventManager().register(this, new ConnectionProtectionListener(logger, proxy, maintenanceService));
        proxy.getEventManager().register(this, new GalacticfyMotdProvider(maintenanceService));
        proxy.getEventManager().register(
                this,
                new MaintenanceListener(maintenanceService, logger)
        );


        logger.info("GalacticfyCore: Commands & Listener registriert.");
    }

}
