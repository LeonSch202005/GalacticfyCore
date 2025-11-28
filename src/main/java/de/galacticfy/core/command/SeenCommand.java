package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.service.SessionService;
import de.galacticfy.core.service.SessionService.SessionInfo;
import net.kyori.adventure.text.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SeenCommand implements SimpleCommand {

    private static final String PERM_SEEN = "galacticfy.core.seen";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final SessionService sessions;
    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withLocale(Locale.GERMANY)
                    .withZone(ZoneId.systemDefault());

    public SeenCommand(ProxyServer proxy,
                       GalacticfyPermissionService perms,
                       SessionService sessions) {
        this.proxy = proxy;
        this.perms = perms;
        this.sessions = sessions;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasPermission(invocation)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text("§eBenutzung: §b/seen <Spieler>")));
            return;
        }

        String target = args[0];

        Optional<Player> opt = proxy.getPlayer(target);
        if (opt.isEmpty()) {
            src.sendMessage(prefix().append(Component.text(
                    "§7Der Spieler §e" + target + " §7war entweder noch nie online oder ist aktuell nicht bekannt."
            )));
            return;
        }

        Player player = opt.get();
        SessionInfo info = sessions.getSession(player.getUniqueId());

        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§b/seen §7Informationen für §f" + player.getUsername())));

        if (info == null) {
            src.sendMessage(Component.text("§7Keine Session-Daten gefunden."));
            src.sendMessage(Component.text(" "));
            return;
        }

        src.sendMessage(Component.text("§8» §7Erster Login: §f" +
                (info.firstLogin() != null ? fmt.format(info.firstLogin()) : "unbekannt")));
        src.sendMessage(Component.text("§8» §7Letzter Login: §f" +
                (info.lastLogin() != null ? fmt.format(info.lastLogin()) : "unbekannt")));
        src.sendMessage(Component.text("§8» §7Letzter Logout: §f" +
                (info.lastLogout() != null ? fmt.format(info.lastLogout()) : "aktuell online?")));
        src.sendMessage(Component.text("§8» §7Gesamtspielzeit: §f" +
                SessionService.formatDuration(info.totalPlaySeconds())));
        src.sendMessage(Component.text("§8» §7Letzter Server: §f" +
                (info.lastServer() != null ? info.lastServer() : "unbekannt")));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource src = invocation.source();
        if (src instanceof Player p) {
            return perms != null
                    ? perms.hasPluginPermission(p, PERM_SEEN)
                    : p.hasPermission(PERM_SEEN);
        }
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        // /seen <Spieler>
        if (args.length == 0) {
            // alle Spieler vorschlagen
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        if (args.length == 1) {
            String prefix = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        return List.of();
    }

}
