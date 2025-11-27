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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class WarningsCommand implements SimpleCommand {

    private static final String PERM_WARNINGS = "galacticfy.punish.warnings";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;

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

        UUID uuid = proxy.getPlayer(targetName)
                .map(Player::getUniqueId)
                .orElse(null);

        // Du brauchst in PunishmentService:
        // List<Punishment> getWarns(UUID uuid, String name, int limit)
        List<Punishment> warns = punishmentService.getWarns(uuid, targetName, 100);

        warns = warns.stream()
                .filter(p -> p.type == PunishmentType.WARN)
                .sorted(Comparator.comparing(p -> p.createdAt))
                .collect(Collectors.toList());

        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§bWarns für §f" + targetName)));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));

        if (warns.isEmpty()) {
            src.sendMessage(Component.text("§7Keine Warns vorhanden."));
        } else {
            int i = 1;
            for (Punishment p : warns) {
                String date = p.createdAt != null
                        ? DATE_FORMAT.format(LocalDateTime.ofInstant(p.createdAt, ZoneId.systemDefault()))
                        : "-";

                src.sendMessage(Component.text(
                        "§8#§e" + i + " §7am §f" + date +
                                " §8| §7Von: §f" + p.staff +
                                "\n   §7Grund: §f" + p.reason
                ));
                i++;
            }
        }

        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasWarningsPermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasWarningsPermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            Set<String> names = new LinkedHashSet<>();
            names.addAll(punishmentService.getAllPunishedNames());
            proxy.getAllPlayers().forEach(p -> names.add(p.getUsername()));
            return names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            Set<String> names = new LinkedHashSet<>();
            names.addAll(punishmentService.getAllPunishedNames());
            proxy.getAllPlayers().forEach(p -> names.add(p.getUsername()));

            return names.stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        return List.of();
    }
}
