package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.ServerTeleportService;
import net.kyori.adventure.text.Component;

public class HubCommand implements SimpleCommand {

    private final ServerTeleportService teleportService;

    public HubCommand(ServerTeleportService teleportService) {
        this.teleportService = teleportService;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Dieser Befehl ist nur für Spieler."));
            return;
        }

        teleportService.sendToServer(player, "Lobby-1", "der Lobby", false);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
