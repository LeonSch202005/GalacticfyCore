package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.PunishDesign;
import de.galacticfy.core.punish.ReasonPresets;
import de.galacticfy.core.punish.ReasonPresets.Preset;
import de.galacticfy.core.service.ReportService;
import de.galacticfy.core.service.ReportService.ReportEntry;
import net.kyori.adventure.text.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportCommand implements SimpleCommand {

    private static final String PERM_REPORT_VIEW  = "galacticfy.report.view";   // check/list + Join-Info
    private static final String PERM_REPORT_CLEAR = "galacticfy.report.clear";  // clear/all

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public ReportCommand(ProxyServer proxy,
                         GalacticfyPermissionService perms,
                         ReportService reportService) {
        this.proxy = proxy;
        this.perms = perms;
        this.reportService = reportService;
    }

    private Component prefix() {
        return Component.text("§8[§cReport§8] §r");
    }

    private boolean canViewReports(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_REPORT_VIEW);
            }
            return p.hasPermission(PERM_REPORT_VIEW);
        }
        return true; // Konsole darf alles
    }

    private boolean canClearReports(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_REPORT_CLEAR)
                        || perms.hasPluginPermission(p, PERM_REPORT_VIEW);
            }
            return p.hasPermission(PERM_REPORT_CLEAR) || p.hasPermission(PERM_REPORT_VIEW);
        }
        return true;
    }

    // =====================================================================================
    // EXECUTE
    // =====================================================================================

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendUsage(src);
            return;
        }

        String first = args[0].toLowerCase(Locale.ROOT);

        // Staff-Subcommands
        if (first.equals("check")) {
            if (!canViewReports(src)) {
                src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
                return;
            }
            handleCheck(src, args);
            return;
        }

        if (first.equals("list")) {
            if (!canViewReports(src)) {
                src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
                return;
            }
            handleList(src);
            return;
        }

        if (first.equals("clear")) {
            if (!canClearReports(src)) {
                src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
                return;
            }
            handleClear(src, args);
            return;
        }

        // Spieler-Report: /report <Spieler> <Grund/Preset...>
        handlePlayerReport(src, args);
    }

    // =====================================================================================
    // /report <Spieler> <Grund/Preset...>
    // =====================================================================================

    private void handlePlayerReport(CommandSource src, String[] args) {
        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/report <Spieler> <Grund oder Preset>"
            )));
            src.sendMessage(Component.text("§7Beispiele:"));
            src.sendMessage(Component.text("§8» §b/report Spieler beleidigung"));
            src.sendMessage(Component.text("§8» §b/report Spieler spam"));
            src.sendMessage(Component.text("§8» §b/report Spieler hackclient KillAura und Fly"));
            return;
        }

        String targetName = args[0];

        String rawSecond = args[1].toLowerCase(Locale.ROOT);
        Preset preset = ReasonPresets.find(rawSecond);

        String reason;
        String presetKey = null;

        if (preset != null) {
            presetKey = preset.key();

            if (args.length > 2) {
                String extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                reason = preset.display() + " §8(§7" + extra + "§8)";
            } else {
                reason = preset.display();
            }
        } else {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        String reporterName;
        String serverName = "Unbekannt";

        if (src instanceof Player p) {
            reporterName = p.getUsername();
            serverName = p.getCurrentServer()
                    .map(conn -> conn.getServerInfo().getName())
                    .orElse("Unbekannt");
        } else {
            reporterName = "Konsole";
        }

        // In DB speichern
        reportService.addReport(targetName, reporterName, reason, serverName, presetKey);

        // Spieler-Feedback
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text(
                "§aDein Report gegen §e" + targetName + " §awurde an das Team gesendet."
        )));
        src.sendMessage(Component.text("§7Grund: §f" + reason));
        src.sendMessage(Component.text(" "));

        // Team benachrichtigen
        notifyStaffNewReport(targetName, reporterName, serverName, reason, presetKey);
    }

    private void notifyStaffNewReport(String targetName,
                                      String reporterName,
                                      String serverName,
                                      String reason,
                                      String presetKey) {

        String presetPart = (presetKey != null)
                ? " §8[§bPreset: §f" + presetKey + "§8]"
                : "";

        Component msg = Component.text(
                "§8[§cReport§8] §7" + reporterName +
                        " §7hat §c" + targetName + " §7gemeldet §8(§b" +
                        serverName + "§8)" + presetPart + "\n" +
                        "§7Grund: §f" + reason
        );

        proxy.getAllPlayers().forEach(player -> {
            boolean view;
            if (perms != null) {
                view = perms.hasPluginPermission(player, PERM_REPORT_VIEW);
            } else {
                view = player.hasPermission(PERM_REPORT_VIEW);
            }

            if (view) {
                player.sendMessage(msg);
            }
        });
    }

    // =====================================================================================
    // /report check <Spieler>
    // =====================================================================================

    private void handleCheck(CommandSource src, String[] args) {
        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/report check <Spieler>"
            )));
            return;
        }

        String targetName = args[1];
        List<ReportEntry> list = reportService.getReportsFor(targetName);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(prefix().append(Component.text("§bReports für §f" + targetName)));
        src.sendMessage(Component.text(PunishDesign.LINE));

        if (list.isEmpty()) {
            src.sendMessage(Component.text("§7Es liegen aktuell §ckeine§7 Reports für diesen Spieler vor."));
        } else {
            for (ReportEntry entry : list) {
                String time = DATE_FORMAT.format(entry.createdAt());

                String presetPart = (entry.presetKey() != null)
                        ? " §8[§b" + entry.presetKey() + "§8]"
                        : "";

                src.sendMessage(Component.text(
                        "§8• §7Am §f" + time +
                                " §8| §7Von: §f" + entry.reporterName() +
                                " §8| §7Server: §f" + (entry.serverName() != null ? entry.serverName() : "Unbekannt") +
                                presetPart +
                                "\n    §7Grund: §f" + entry.reason()
                ));
            }
        }

        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));
    }

    // =====================================================================================
    // /report list
    // =====================================================================================

    private void handleList(CommandSource src) {
        List<ReportEntry> all = reportService.getAllReports();

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(prefix().append(Component.text("§bOffene Reports")));
        src.sendMessage(Component.text(PunishDesign.LINE));

        if (all.isEmpty()) {
            src.sendMessage(Component.text("§7Aktuell liegen §ckeine§7 offenen Reports vor."));
            src.sendMessage(Component.text(PunishDesign.LINE));
            src.sendMessage(Component.text(" "));
            return;
        }

        for (ReportEntry entry : all) {
            String time = DATE_FORMAT.format(entry.createdAt());

            String presetPart = (entry.presetKey() != null)
                    ? " §8[§b" + entry.presetKey() + "§8]"
                    : "";

            src.sendMessage(Component.text(
                    "§8• §c" + entry.targetName() + " §7(" + time + ") " +
                            "§8| §7Von: §f" + entry.reporterName() +
                            " §8| §7Server: §f" + (entry.serverName() != null ? entry.serverName() : "Unbekannt") +
                            presetPart +
                            "\n    §7Grund: §f" + entry.reason()
            ));
        }

        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));
    }

    // =====================================================================================
    // /report clear <Spieler|all>
    // =====================================================================================

    private void handleClear(CommandSource src, String[] args) {
        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/report clear <Spieler|all>"
            )));
            return;
        }

        String target = args[1];

        if (target.equalsIgnoreCase("all")) {
            int removed = reportService.clearAll();
            src.sendMessage(prefix().append(Component.text(
                    "§aEs wurden §e" + removed + " §aReports gelöscht."
            )));
            return;
        }

        boolean ok = reportService.clearReportsFor(target);

        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§aAlle Reports für §e" + target + " §awurden gelöscht."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§7Es wurden keine Reports für §e" + target + " §7gefunden."
            )));
        }
    }

    // =====================================================================================
    // USAGE
    // =====================================================================================

    private void sendUsage(CommandSource src) {
        boolean staff = canViewReports(src);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m──────§r §cReport §7| §cSystem §8§m──────"));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bFür Spieler:"));
        src.sendMessage(Component.text("§8» §b/report <Spieler> <Grund oder Preset>"));
        src.sendMessage(Component.text("   §7Meldet einen Spieler an das Team."));
        src.sendMessage(Component.text("   §7Presets: §fspam§7, §fbeleidigung§7, §fhackclient§7, §fwerbung§7, ..."));
        src.sendMessage(Component.text(" "));

        if (staff) {
            src.sendMessage(Component.text("§bFür Teammitglieder:"));
            src.sendMessage(Component.text("§8» §b/report check <Spieler>"));
            src.sendMessage(Component.text("   §7Zeigt alle Reports für einen Spieler."));
            src.sendMessage(Component.text("§8» §b/report list"));
            src.sendMessage(Component.text("   §7Zeigt alle offenen Reports."));
            src.sendMessage(Component.text("§8» §b/report clear <Spieler|all>"));
            src.sendMessage(Component.text("   §7Löscht Reports für einen Spieler oder alle."));
            src.sendMessage(Component.text(" "));
        }

        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    // =====================================================================================
    // TAB-COMPLETE
    // =====================================================================================

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        boolean staffView = canViewReports(src);
        boolean staffClear = canClearReports(src);

        // /report
        if (args.length == 0) {
            List<String> out = new ArrayList<>();

            if (staffView) {
                out.add("check");
                out.add("list");
                if (staffClear) {
                    out.add("clear");
                }
            }

            // plus alle Online-Spieler
            out.addAll(proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList());

            return out;
        }

        // /report <arg1>
        if (args.length == 1) {
            String first = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();

            if (staffView) {
                for (String sub : List.of("check", "list", "clear")) {
                    if (sub.startsWith(first)) {
                        out.add(sub);
                    }
                }
            }

            // Spieler-Namen
            proxy.getAllPlayers().forEach(p -> {
                String name = p.getUsername();
                if (name.toLowerCase(Locale.ROOT).startsWith(first)) {
                    out.add(name);
                }
            });

            return out.stream()
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        // /report check <Spieler>
        if (args.length == 2 && args[0].equalsIgnoreCase("check") && staffView) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            Set<String> out = new LinkedHashSet<>();

            // bereits reportete Spieler
            reportService.getReportedTargetNames().forEach(name -> {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(name);
                }
            });

            // plus aktuell Online
            proxy.getAllPlayers().forEach(p -> {
                String name = p.getUsername();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(name);
                }
            });

            return new ArrayList<>(out);
        }

        // /report clear <Spieler|all>
        if (args.length == 2 && args[0].equalsIgnoreCase("clear") && staffClear) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> base = new ArrayList<>();
            base.add("all");
            base.addAll(reportService.getReportedTargetNames());

            return base.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        // /report <Spieler> <Grund/Preset...>
        if (args.length >= 2 && !args[0].equalsIgnoreCase("check")
                && !args[0].equalsIgnoreCase("list")
                && !args[0].equalsIgnoreCase("clear")) {

            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);

            // Presets als Vorschlag
            return ReasonPresets.allKeys().stream()
                    .filter(key -> key.startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // /report soll für JEDEN Tabbar sein → Permission nicht beschränken
        return true;
    }
}
