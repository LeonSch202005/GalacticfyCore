package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.PunishDesign;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;

public class UnbanCommand implements SimpleCommand {

    private static final String PERM_UNBAN = "galacticfy.punish.unban";

    private final PunishmentService punishmentService;
    private final GalacticfyPermissionService perms;

    public UnbanCommand(PunishmentService punishmentService,
                        GalacticfyPermissionService perms) {
        this.punishmentService = punishmentService;
        this.perms = perms;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasUnbanPermission(CommandSource src) {
        if (src instanceof com.velocitypowered.api.proxy.Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_UNBAN);
            }
            return p.hasPermission(PERM_UNBAN);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasUnbanPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/unban <Spieler|IP>"
            )));
            return;
        }

        String targetOrIp = args[0];
        String staffName = (src instanceof com.velocitypowered.api.proxy.Player p)
                ? p.getUsername()
                : "Konsole";

        // Ganz simpel: IP = enthält Punkt → IP-Unban
        Punishment result;

        if (targetOrIp.contains(".")) {
            // Du musst in PunishmentService z.B. implementieren:
            // Punishment unbanByIp(String ip, String staffName)
            result = punishmentService.unbanByIp(targetOrIp, staffName);
        } else {
            // Und hier:
            // Punishment unbanByName(String name, String staffName)
            result = punishmentService.unbanByName(targetOrIp, staffName);
        }

        if (result == null) {
            src.sendMessage(prefix().append(Component.text(
                    "§cEs wurde kein aktiver Ban für §e" + targetOrIp + " §cgefunden."
            )));
            return;
        }

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_UNBAN));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Ziel:   §f" + targetOrIp));
        src.sendMessage(Component.text("§7Von:    §f" + staffName));
        src.sendMessage(Component.text("§7Typ:    §f" + result.type));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasUnbanPermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasUnbanPermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            // Einfach alle bekannten punished Namen
            return punishmentService.getAllPunishedNames();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);

            return punishmentService.getAllPunishedNames().stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        return List.of();
    }
}
