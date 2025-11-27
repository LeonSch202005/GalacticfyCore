package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.service.PunishmentService;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

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
        if (src instanceof com.velocitypowered.api.proxy.Player player) {
            if (perms != null) {
                return perms.hasPluginPermission(player, PERM_UNBAN);
            }
            return player.hasPermission(PERM_UNBAN);
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
                    "§eBenutzung: §b/unban <Spieler>"
            )));
            return;
        }

        String name = args[0];

        boolean ok = punishmentService.unbanByName(name);
        if (!ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§cKein aktiver Ban für §e" + name + " §cgefunden."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§aBan für §e" + name + " §awurde aufgehoben."
            )));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasUnbanPermission(src)) {
            return List.of();
        }

        // /unban <Spieler>  – ohne Prefix alle aktiven Bans
        if (args.length == 0) {
            return punishmentService.getActiveBannedNames().stream()
                    .filter(Objects::nonNull)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return punishmentService.getActiveBannedNames().stream()
                    .filter(Objects::nonNull)
                    .filter(n -> prefix.isEmpty()
                            || n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
