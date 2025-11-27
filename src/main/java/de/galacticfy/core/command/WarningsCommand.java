package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import de.galacticfy.core.service.PunishmentService.PunishmentType;
import net.kyori.adventure.text.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class WarningsCommand implements SimpleCommand {

    private static final String PERM_WARNINGS = "galacticfy.punish.warnings";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public WarningsCommand(ProxyServer proxy,
                           GalacticfyPermissionService perms,
                           PunishmentService punishmentService) {
        this.proxy = proxy;
        this.perms = perms;
        this.punishmentService = punishmentService;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasWarningsPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_WARNINGS);
            }
            return p.hasPermission(PERM_WARNINGS);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasWarningsPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/warnings <Spieler>"
            )));
            return;
        }

        String targetName = args[0];
        Player online = proxy.getPlayer(targetName).orElse(null);
        UUID uuid = online != null ? online.getUniqueId() : null;

        List<Punishment> history = punishmentService.getHistory(uuid, targetName, 100);

        List<Punishment> warns = history.stream()
                .filter(p -> p.type == PunishmentType.WARN)
                .sorted(Comparator.comparing((Punishment p) -> p.createdAt).reversed())
                .collect(Collectors.toList());

        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§bWarnungen für §f" + targetName)));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));

        if (warns.isEmpty()) {
            src.sendMessage(Component.text("§7Keine Verwarnungen gefunden."));
        } else {
            int index = 1;
            for (Punishment p : warns) {

                String date = "-";
                if (p.createdAt != null) {
                    date = DATE_FORMAT.format(p.createdAt.atZone(ZoneId.systemDefault()));
                }

                src.sendMessage(Component.text(
                        "§e#" + index + " §7am §f" + date +
                                " §8| §7von §f" + p.staff +
                                (p.active ? " §8| §aAKTIV" : " §8| §7inaktiv") +
                                "\n    §7Grund: §f" + p.reason
                ));
                index++;
            }
        }

        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasWarningsPermission(src)) {
            return List.of();
        }

        // /warnings <Spieler>  (auch ohne Buchstabe)
        if (args.length == 0 || args.length == 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

            Set<String> result = new LinkedHashSet<>();
            result.addAll(punishmentService.findKnownNames(prefix, 30));
            proxy.getAllPlayers().forEach(p -> {
                String n = p.getUsername();
                if (n.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(n);
                }
            });
            return new ArrayList<>(result);
        }

        return List.of();
    }
}
