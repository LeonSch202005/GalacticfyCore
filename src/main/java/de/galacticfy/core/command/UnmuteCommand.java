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

public class UnmuteCommand implements SimpleCommand {

    private static final String PERM_UNMUTE = "galacticfy.punish.unmute";

    private final PunishmentService punishmentService;
    private final GalacticfyPermissionService perms;

    public UnmuteCommand(PunishmentService punishmentService,
                         GalacticfyPermissionService perms) {
        this.punishmentService = punishmentService;
        this.perms = perms;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasUnmutePermission(CommandSource src) {
        if (src instanceof com.velocitypowered.api.proxy.Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_UNMUTE);
            }
            return p.hasPermission(PERM_UNMUTE);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasUnmutePermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/unmute <Spieler>"
            )));
            return;
        }

        String targetName = args[0];
        String staffName = (src instanceof com.velocitypowered.api.proxy.Player p)
                ? p.getUsername()
                : "Konsole";

        // Du brauchst in PunishmentService z.B.:
        // Punishment unmuteByName(String name, String staffName)
        Punishment result = punishmentService.unmuteByName(targetName, staffName);

        if (result == null) {
            src.sendMessage(prefix().append(Component.text(
                    "§cEs wurde kein aktiver Mute für §e" + targetName + " §cgefunden."
            )));
            return;
        }

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_UNMUTE));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Spieler: §f" + targetName));
        src.sendMessage(Component.text("§7Von:     §f" + staffName));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasUnmutePermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasUnmutePermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
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
