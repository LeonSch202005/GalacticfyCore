package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.MaintenanceService;
import de.galacticfy.core.service.ServerTeleportService;
import net.kyori.adventure.text.Component;

public class EventCommand implements SimpleCommand {

    private final ServerTeleportService teleportService;
    private final MaintenanceService maintenanceService;

    private static final String BACKEND_NAME = "event-1";

    public EventCommand(ServerTeleportService teleportService,
                        MaintenanceService maintenanceService) {
        this.teleportService = teleportService;
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Dieser Befehl ist nur für Spieler."));
            return;
        }

        if (!player.hasPermission("galacticfy.event.join")) {
            player.sendMessage(Component.text("§cDu hast keine Berechtigung, dem Event beizutreten."));
            return;
        }

        if (maintenanceService.isServerInMaintenance(BACKEND_NAME)) {
            player.sendMessage(Component.text(
                    "§cDas Event befindet sich derzeit im Wartungsmodus.§7 Du kannst es momentan nicht betreten."
            ));
            return;
        }

        teleportService.sendToServer(player, BACKEND_NAME, "dem Event", false);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // eigentliche Permission prüfen wir oben
    }
}
