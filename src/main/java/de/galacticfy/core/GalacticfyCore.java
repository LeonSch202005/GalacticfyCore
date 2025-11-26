package de.galacticfy.core;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.command.*;
import de.galacticfy.core.database.DatabaseManager;
import de.galacticfy.core.database.DatabaseMigrationService;
import de.galacticfy.core.listener.*;
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

    private ServerTeleportService teleportService;
    private MaintenanceService maintenanceService;

    private DatabaseManager databaseManager;
    private GalacticfyPermissionService permissionService;
    private DiscordWebhookNotifier discordNotifier;

    @Inject
    public GalacticfyCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GalacticfyCore wird initialisiert...");

        // DB
        this.databaseManager = new DatabaseManager(logger);
        this.databaseManager.init();
        new DatabaseMigrationService(databaseManager, logger).runMigrations();

        // Services
        this.teleportService = new ServerTeleportService(proxy, logger);
        this.maintenanceService = new MaintenanceService(logger, databaseManager);

        // Eigenes Permission- / Rollen-System
        this.permissionService = new GalacticfyPermissionService(databaseManager, logger);

        // Discord-Webhook (TODO: später aus Config lesen)
        String webhookUrl = "DEIN_WEBHOOK_HIER";
        this.discordNotifier = new DiscordWebhookNotifier(logger, webhookUrl);

        CommandManager commandManager = proxy.getCommandManager();

        // Teleport-Commands
        CommandMeta hubMeta = commandManager.metaBuilder("hub")
                .aliases("lobby", "spawn")
                .build();
        commandManager.register(hubMeta, new HubCommand(teleportService, maintenanceService));

        CommandMeta cbMeta = commandManager.metaBuilder("citybuild")
                .aliases("cb")
                .build();
        commandManager.register(cbMeta, new CitybuildCommand(teleportService, maintenanceService));

        CommandMeta sbMeta = commandManager.metaBuilder("skyblock")
                .aliases("sb")
                .build();
        commandManager.register(sbMeta, new SkyblockCommand(teleportService, maintenanceService));

        CommandMeta eventMeta = commandManager.metaBuilder("event")
                .build();
        commandManager.register(eventMeta, new EventCommand(teleportService, maintenanceService, permissionService));

        CommandMeta sendMeta = commandManager.metaBuilder("send")
                .build();
        commandManager.register(sendMeta, new SendCommand(proxy, teleportService, permissionService));

        // Maintenance (EN / DE Layout)
        CommandMeta maintenanceMeta = commandManager.metaBuilder("maintenance").build();
        commandManager.register(
                maintenanceMeta,
                new MaintenanceCommand(maintenanceService, proxy, discordNotifier, false, permissionService)
        );

        CommandMeta wartungMeta = commandManager.metaBuilder("wartung").build();
        commandManager.register(
                wartungMeta,
                new MaintenanceCommand(maintenanceService, proxy, discordNotifier, true, permissionService)
        );

        // Rank / Rollen-Verwaltung
        CommandMeta rankMeta = commandManager.metaBuilder("rank").build();
        commandManager.register(rankMeta, new RankCommand(permissionService, proxy));

        // Listener
        proxy.getEventManager().register(this,
                new ConnectionProtectionListener(logger, proxy, maintenanceService));
        proxy.getEventManager().register(this,
                new GalacticfyMotdProvider(maintenanceService));
        proxy.getEventManager().register(this,
                new MaintenanceListener(maintenanceService, logger, permissionService));
        proxy.getEventManager().register(this,
                new PermissionsSetupListener(permissionService, logger));

        // Tablist (Header/Footer + Prefix aus Rank-System)
        proxy.getEventManager().register(this,
                new TablistPrefixListener(proxy, permissionService, logger));

        logger.info("GalacticfyCore: Commands & Listener registriert.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("GalacticfyCore fährt herunter, schließe Ressourcen...");

        if (maintenanceService != null) {
            maintenanceService.shutdown();
        }

        if (discordNotifier != null) {
            discordNotifier.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        logger.info("GalacticfyCore: Shutdown abgeschlossen.");
    }
}
