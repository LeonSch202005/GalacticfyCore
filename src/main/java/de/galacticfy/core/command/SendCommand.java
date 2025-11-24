package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.service.ServerTeleportService;
import net.kyori.adventure.text.Component;

import java.util.List;
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
        teleportService.sendToServer(target, backendName, backendName, true); // Staff: ohne Cooldown
        invocation.source().sendMessage(Component.text("§aSende §e" + target.getUsername() + " §aan Server §e" + backendName + "§a."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        // Tab-Complete: Servernamen beim 2. Argument
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return proxy.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
