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
        if (src instanceof com.velocitypowered.api.proxy.Player player) {
            if (perms != null) {
                return perms.hasPluginPermission(player, PERM_UNMUTE);
            }
            return player.hasPermission(PERM_UNMUTE);
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

        String name = args[0];

        boolean ok = punishmentService.unmuteByName(name);
        if (!ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§cKein aktiver Mute für §e" + name + " §cgefunden."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§aMute für §e" + name + " §awurde aufgehoben."
            )));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasUnmutePermission(src)) {
            return List.of();
        }

        // /unmute <Spieler>
        if (args.length == 0) {
            return punishmentService.getActiveMutedNames().stream()
                    .filter(Objects::nonNull)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return punishmentService.getActiveMutedNames().stream()
                    .filter(Objects::nonNull)
                    .filter(n -> prefix.isEmpty()
                            || n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
