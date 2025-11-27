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

public class CheckCommand implements SimpleCommand {

    private static final String PERM_CHECK = "galacticfy.punish.check";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public CheckCommand(ProxyServer proxy,
                        GalacticfyPermissionService perms,
                        PunishmentService punishmentService) {
        this.proxy = proxy;
        this.perms = perms;
        this.punishmentService = punishmentService;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasCheckPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_CHECK);
            }
            return p.hasPermission(PERM_CHECK);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasCheckPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/check <Spieler>"
            )));
            return;
        }

        String targetName = args[0];

        Player online = proxy.getPlayer(targetName).orElse(null);
        UUID uuid = online != null ? online.getUniqueId() : null;

        List<Punishment> history = punishmentService.getHistory(uuid, targetName, 50);

        Optional<Punishment> activeBan = history.stream()
                .filter(p -> (p.type == PunishmentType.BAN || p.type == PunishmentType.IP_BAN) && p.active)
                .findFirst();

        Optional<Punishment> activeMute = history.stream()
                .filter(p -> p.type == PunishmentType.MUTE && p.active)
                .findFirst();

        List<Punishment> warns = history.stream()
                .filter(p -> p.type == PunishmentType.WARN)
                .sorted(Comparator.comparing((Punishment p) -> p.createdAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<Punishment> lastEntries = history.stream()
                .sorted(Comparator.comparing((Punishment p) -> p.createdAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§bCheck für §f" + targetName)));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));

        // Online-Status
        if (online != null) {
            String serverName = online.getCurrentServer()
                    .map(c -> c.getServerInfo().getName())
                    .orElse("Unbekannt");
            src.sendMessage(Component.text("§7Status: §aonline §8(§7" + serverName + "§8)"));
        } else {
            src.sendMessage(Component.text("§7Status: §coffline"));
        }

        src.sendMessage(Component.text(" "));

        // Aktive Strafen
        src.sendMessage(Component.text("§b§lAktive Strafen"));

        if (activeBan.isEmpty() && activeMute.isEmpty()) {
            src.sendMessage(Component.text("§7Keine aktiven Bans/Mutes."));
        } else {
            activeBan.ifPresent(p -> {
                String date = p.createdAt != null
                        ? DATE_FORMAT.format(p.createdAt.atZone(ZoneId.systemDefault()))
                        : "-";
                String duration = punishmentService.formatRemaining(p);
                src.sendMessage(Component.text(
                        "§c⛔ BAN §8| §7seit §f" + date +
                                " §8| §7Dauer: §f" + duration +
                                "\n    §7Grund: §f" + p.reason
                ));
            });

            activeMute.ifPresent(p -> {
                String date = p.createdAt != null
                        ? DATE_FORMAT.format(p.createdAt.atZone(ZoneId.systemDefault()))
                        : "-";
                String duration = punishmentService.formatRemaining(p);
                src.sendMessage(Component.text(
                        "§6🔇 MUTE §8| §7seit §f" + date +
                                " §8| §7Dauer: §f" + duration +
                                "\n    §7Grund: §f" + p.reason
                ));
            });
        }

        src.sendMessage(Component.text(" "));

        // Warnungen
        src.sendMessage(Component.text("§b§lLetzte Verwarnungen"));
        if (warns.isEmpty()) {
            src.sendMessage(Component.text("§7Keine Verwarnungen gefunden."));
        } else {
            for (Punishment p : warns) {
                String date = p.createdAt != null
                        ? DATE_FORMAT.format(p.createdAt.atZone(ZoneId.systemDefault()))
                        : "-";
                src.sendMessage(Component.text(
                        "§e⚠ §7am §f" + date +
                                " §8| §7von §f" + p.staff +
                                "\n    §7Grund: §f" + p.reason
                ));
            }
        }

        src.sendMessage(Component.text(" "));

        // Kurz-History
        src.sendMessage(Component.text("§b§lLetzte Aktionen"));
        if (lastEntries.isEmpty()) {
            src.sendMessage(Component.text("§7Keine Einträge gefunden."));
        } else {
            for (Punishment p : lastEntries) {
                String icon;
                String color;

                switch (p.type) {
                    case BAN -> { icon = "⛔"; color = "§c"; }
                    case IP_BAN -> { icon = "🖥"; color = "§4"; }
                    case MUTE -> { icon = "🔇"; color = "§6"; }
                    case KICK -> { icon = "👢"; color = "§e"; }
                    case WARN -> { icon = "⚠"; color = "§e"; }
                    default -> { icon = "❔"; color = "§7"; }
                }

                String date = p.createdAt != null
                        ? DATE_FORMAT.format(p.createdAt.atZone(ZoneId.systemDefault()))
                        : "-";
                String duration = punishmentService.formatRemaining(p);

                src.sendMessage(Component.text(
                        color + icon + " §7" + p.type.name() +
                                " §8| §7am §f" + date +
                                " §8| §7von §f" + p.staff +
                                " §8| §7Dauer: §f" + duration +
                                "\n    §7Grund: §f" + p.reason
                ));
            }
        }

        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasCheckPermission(src)) {
            return List.of();
        }

        // /check <Spieler>
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
