package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.ServerTeleportService;
import net.kyori.adventure.text.Component;

public class EventCommand implements SimpleCommand {

    private final ServerTeleportService teleportService;

    public EventCommand(ServerTeleportService teleportService) {
        this.teleportService = teleportService;
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

        // Backend-Namen ggf. anpassen ("event-1" o.ä.)
        teleportService.sendToServer(player, "event-1", "dem Event", false);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // eigentliche Permission prüfen wir oben
    }
}
