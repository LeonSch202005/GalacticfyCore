package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import de.galacticfy.core.service.MaintenanceService;
import net.kyori.adventure.text.Component;

public class MaintenanceCommand implements SimpleCommand {

    private final MaintenanceService maintenanceService;

    public MaintenanceCommand(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();

        if (!source.hasPermission("galacticfy.maintenance.toggle")) {
            source.sendMessage(Component.text("§cDazu hast du keine Berechtigung."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            source.sendMessage(Component.text("§eBenutzung: /maintenance <on|off|toggle|status>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                maintenanceService.setMaintenanceEnabled(true);
                source.sendMessage(Component.text("§aMaintenance wurde §caktiviert§a."));
            }
            case "off" -> {
                maintenanceService.setMaintenanceEnabled(false);
                source.sendMessage(Component.text("§aMaintenance wurde §adeaktiviert§a."));
            }
            case "toggle" -> {
                maintenanceService.toggle();
                boolean enabled = maintenanceService.isMaintenanceEnabled();
                source.sendMessage(Component.text("§aMaintenance ist jetzt: " + (enabled ? "§cAN" : "§aAUS")));
            }
            case "status" -> {
                boolean enabled = maintenanceService.isMaintenanceEnabled();
                source.sendMessage(Component.text("§7Maintenance-Status: " + (enabled ? "§cAN" : "§aAUS")));
            }
            default -> source.sendMessage(Component.text("§eBenutzung: /maintenance <on|off|toggle|status>"));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // eigentliche Permission prüfe ich oben nochmal explizit
        return invocation.source().hasPermission("galacticfy.maintenance.toggle");
    }
}
