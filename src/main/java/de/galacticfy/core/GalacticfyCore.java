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
import de.galacticfy.core.service.*;
import de.galacticfy.core.util.DiscordWebhookNotifier;
import org.slf4j.Logger;

@Plugin(
        id = "galacticfycore",
        name = "GalacticfyCore",
        version = "0.4.5",
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

    private PunishmentService punishmentService;
    private ReportService reportService;

    private MessageService messageService;
    private AutoBroadcastService autoBroadcastService;

    @Inject
    public GalacticfyCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GalacticfyCore wird initialisiert...");

        // ==============================
        // DB
        // ==============================
        this.databaseManager = new DatabaseManager(logger);
        this.databaseManager.init();
        new DatabaseMigrationService(databaseManager, logger).runMigrations();

        // ==============================
        // Services
        // ==============================
        this.teleportService = new ServerTeleportService(proxy, logger);
        this.maintenanceService = new MaintenanceService(logger, databaseManager);

        this.permissionService = new GalacticfyPermissionService(databaseManager, logger);
        this.punishmentService = new PunishmentService(databaseManager, logger);
        this.reportService = new ReportService(databaseManager, logger);

        this.messageService = new MessageService(proxy, logger);
        this.autoBroadcastService = new AutoBroadcastService(proxy, messageService, logger, this);
        autoBroadcastService.start();

        String webhookUrl = "https://discord.com/api/webhooks/1443274192542765168/aHgrQP2ADryVWfhdoW5dcP7Vd8J_YU9aOkjEVkYNlVc-4wLEnAs-E5e-IfJg0fBwN8dJ";
        this.discordNotifier = new DiscordWebhookNotifier(logger, webhookUrl);

        CommandManager commandManager = proxy.getCommandManager();

        // ==============================
        // Teleport-Commands
        // ==============================
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

        CommandMeta eventMeta = commandManager.metaBuilder("event").build();
        commandManager.register(eventMeta, new EventCommand(teleportService, maintenanceService, permissionService));

        CommandMeta sendMeta = commandManager.metaBuilder("send").build();
        commandManager.register(sendMeta, new SendCommand(proxy, teleportService, permissionService));

        // ==============================
        // Maintenance (EN / DE Layout)
        // ==============================
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

        // ==============================
        // Rank / Rollen-Verwaltung
        // ==============================
        CommandMeta rankMeta = commandManager.metaBuilder("rank").build();
        commandManager.register(rankMeta, new RankCommand(permissionService, proxy));

        // ==============================
        // Punishment-Commands
        // ==============================
        CommandMeta banMeta = commandManager.metaBuilder("ban").build();
        commandManager.register(
                banMeta,
                new BanCommand(proxy, permissionService, punishmentService, discordNotifier)
        );

        CommandMeta banIpMeta = commandManager.metaBuilder("banip").build();
        commandManager.register(
                banIpMeta,
                new BanIpCommand(proxy, permissionService, punishmentService, discordNotifier)
        );

        CommandMeta unbanMeta = commandManager.metaBuilder("unban").build();
        commandManager.register(
                unbanMeta,
                new UnbanCommand(punishmentService, permissionService)
        );

        CommandMeta muteMeta = commandManager.metaBuilder("mute").build();
        commandManager.register(
                muteMeta,
                new MuteCommand(proxy, permissionService, punishmentService, discordNotifier)
        );

        CommandMeta unmuteMeta = commandManager.metaBuilder("unmute").build();
        commandManager.register(
                unmuteMeta,
                new UnmuteCommand(punishmentService, permissionService)
        );

        CommandMeta kickMeta = commandManager.metaBuilder("kick").build();
        commandManager.register(
                kickMeta,
                new KickCommand(proxy, permissionService, punishmentService, discordNotifier)
        );

        CommandMeta historyMeta = commandManager.metaBuilder("history").build();
        commandManager.register(
                historyMeta,
                new HistoryCommand(proxy, punishmentService, permissionService)
        );

        CommandMeta checkMeta = commandManager.metaBuilder("check").build();
        commandManager.register(
                checkMeta,
                new CheckCommand(proxy, permissionService, punishmentService)
        );

        CommandMeta warningsMeta = commandManager.metaBuilder("warnings").build();
        commandManager.register(
                warningsMeta,
                new WarningsCommand(proxy, permissionService, punishmentService)
        );

        CommandMeta warnMeta = commandManager.metaBuilder("warn").build();
        commandManager.register(
                warnMeta,
                new WarnCommand(proxy, permissionService, punishmentService, discordNotifier)
        );

        CommandMeta reportMeta = commandManager.metaBuilder("report").build();
        commandManager.register(
                reportMeta,
                new ReportCommand(proxy, permissionService, reportService)
        );

        // ==============================
        // NEW: Broadcast / Alert / Announce
        // ==============================
        CommandMeta alertMeta = commandManager.metaBuilder("alert").build();
        commandManager.register(
                alertMeta,
                new AlertCommand(messageService, permissionService)
        );

        CommandMeta announceMeta = commandManager.metaBuilder("announce").build();
        commandManager.register(
                announceMeta,
                new AnnounceCommand(messageService, permissionService)
        );

        CommandMeta broadcastMeta = commandManager.metaBuilder("broadcast")
                .aliases("bc")
                .build();
        commandManager.register(
                broadcastMeta,
                new BroadcastCommand(messageService, permissionService)
        );

        // Staffchat
        CommandMeta staffMeta = commandManager.metaBuilder("staffchat")
                .aliases("sc")
                .build();
        commandManager.register(
                staffMeta,
                new StaffChatCommand(proxy, permissionService)
        );

        CommandMeta unwarnMeta = commandManager.metaBuilder("unwarn").build();
        commandManager.register(
                unwarnMeta,
                new UnwarnCommand(proxy, permissionService, punishmentService)
        );

        // ==============================
        // Listener
        // ==============================
        proxy.getEventManager().register(this,
                new ConnectionProtectionListener(logger, proxy, maintenanceService));

        proxy.getEventManager().register(this,
                new GalacticfyMotdProvider(maintenanceService));

        proxy.getEventManager().register(this,
                new MaintenanceListener(maintenanceService, logger, permissionService));

        proxy.getEventManager().register(this,
                new PermissionsSetupListener(permissionService, logger));

        proxy.getEventManager().register(this,
                new TablistPrefixListener(proxy, permissionService, logger));

        proxy.getEventManager().register(this,
                new PunishmentLoginListener(punishmentService, logger, proxy, permissionService));

        logger.info("GalacticfyCore: Commands, Listener, Punishment-, Report- & AutoBroadcast-System registriert.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("GalacticfyCore fährt herunter, schließe Ressourcen...");

        if (autoBroadcastService != null) {
            autoBroadcastService.shutdown();
        }

        if (maintenanceService != null) {
            maintenanceService.shutdown();
        }

        if (discordNotifier != null) {
            discordNotifier.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        if (punishmentService != null) {
            punishmentService.shutdown();
        }

        logger.info("GalacticfyCore: Shutdown abgeschlossen.");
    }
}
