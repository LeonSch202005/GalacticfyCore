package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.service.ServerTeleportService;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SendCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ServerTeleportService teleportService;

    public SendCommand(ProxyServer proxy, ServerTeleportService teleportService) {
        this.proxy = proxy;
        this.teleportService = teleportService;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("galacticfy.command.send")) {
            invocation.source().sendMessage(Component.text("§cDazu hast du keine Berechtigung."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            invocation.source().sendMessage(Component.text("§eBenutzung: /send <Spieler> <Server>"));
            return;
        }

        String targetName = args[0];
        String backendName = args[1];

        Optional<Player> optional = proxy.getPlayer(targetName);
        if (optional.isEmpty()) {
            invocation.source().sendMessage(Component.text("§cSpieler \"" + targetName + "\" wurde nicht gefunden."));
            return;
        }

        Player target = optional.get();
        teleportService.sendToServer(target, backendName, backendName, true);
        invocation.source().sendMessage(Component.text("§aSende §e" + target.getUsername() + " §aan Server §e" + backendName + "§a."));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("galacticfy.command.send");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission("galacticfy.command.send")) {
            return List.of();
        }

        String[] args = invocation.arguments();

        // Noch keine Argumente getippt: alle Spieler vorschlagen
        if (args.length == 0) {
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .toList();
        }

        // 1. Argument: Spieler
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        // 2. Argument: Server
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (prefix.isEmpty()) {
                return proxy.getAllServers().stream()
                        .map(s -> s.getServerInfo().getName())
                        .toList();
            }

            return proxy.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
