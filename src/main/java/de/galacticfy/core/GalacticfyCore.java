package de.galacticfy.core;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.command.*;
import de.galacticfy.core.database.DatabaseManager;
import de.galacticfy.core.database.DatabaseMigrationService;
import de.galacticfy.core.listener.ConnectionProtectionListener;
import de.galacticfy.core.listener.MaintenanceListener;
import de.galacticfy.core.motd.GalacticfyMotdProvider;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.service.MaintenanceService;
import de.galacticfy.core.service.ServerTeleportService;
import de.galacticfy.core.util.DiscordWebhookNotifier;
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

    private DatabaseManager databaseManager;
    private GalacticfyPermissionService permissionService;
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
        logger.info("GalacticfyCore wird initialisiert...");

        // DB
        this.databaseManager = new DatabaseManager(logger);
        this.databaseManager.init();
        new DatabaseMigrationService(databaseManager, logger).runMigrations();

        // Eigenes Rollen-/Permission-System (ohne LuckPerms)
        this.permissionService = new GalacticfyPermissionService(databaseManager, logger);

        // Discord-Webhook
        String webhookUrl = "DEIN_WEBHOOK_HIER";
        this.discordNotifier = new DiscordWebhookNotifier(logger, webhookUrl);

        CommandManager commandManager = proxy.getCommandManager();

        // Teleport-Commands
        commandManager.register(
                commandManager.metaBuilder("hub").aliases("lobby", "spawn").build(),
                new HubCommand(teleportService, maintenanceService)
        );

        commandManager.register(
                commandManager.metaBuilder("citybuild").aliases("cb").build(),
                new CitybuildCommand(teleportService, maintenanceService)
        );

        commandManager.register(
                commandManager.metaBuilder("skyblock").aliases("sb").build(),
                new SkyblockCommand(teleportService, maintenanceService)
        );

        CommandMeta eventMeta = commandManager.metaBuilder("event")
                .build();
        commandManager.register(eventMeta, new EventCommand(teleportService, maintenanceService, permissionService));

        CommandMeta sendMeta = commandManager.metaBuilder("send")
                .build();
        commandManager.register(sendMeta, new SendCommand(proxy, teleportService, permissionService));


        // Maintenance (EN / DE Layout)
        commandManager.register(
                commandManager.metaBuilder("maintenance").build(),
                new MaintenanceCommand(maintenanceService, proxy, discordNotifier, false, permissionService)
        );

        commandManager.register(
                commandManager.metaBuilder("wartung").build(),
                new MaintenanceCommand(maintenanceService, proxy, discordNotifier, true, permissionService)
        );

        // Rank / Rollen-Verwaltung
        commandManager.register(
                commandManager.metaBuilder("rank").build(),
                new RankCommand(permissionService, proxy)
        );

        // Listener
        proxy.getEventManager().register(this,
                new ConnectionProtectionListener(logger, proxy, maintenanceService));
        proxy.getEventManager().register(this,
                new GalacticfyMotdProvider(maintenanceService));
        proxy.getEventManager().register(this,
                new MaintenanceListener(maintenanceService, logger));

        logger.info("GalacticfyCore: Commands & Listener registriert.");
    }
}
