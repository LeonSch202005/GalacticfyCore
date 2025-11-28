package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.ReportService;
import de.galacticfy.core.service.ReportService.ReportEntry;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ReportsCommand implements SimpleCommand {

    private static final String PERM_REPORTS = "galacticfy.staff.reports";

    private final ReportService reportService;

    public ReportsCommand(ReportService reportService) {
        this.reportService = reportService;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!hasPermission(invocation)) {
            source.sendMessage(prefix().append(
                    Component.text("§cDazu hast du keine Berechtigung.")
            ));
            return;
        }

        if (args.length == 0) {
            listOpenReports(source);
            return;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("handle")) {
            handleReport(source, args[1]);
            return;
        }

        source.sendMessage(prefix().append(
                Component.text("§7Benutzung: §b/reports §7oder §b/reports handle <id>")
        ));
    }

    private void listOpenReports(CommandSource source) {
        List<ReportEntry> open = reportService.getOpenReports();

        if (open.isEmpty()) {
            source.sendMessage(prefix().append(
                    Component.text("§7Es gibt aktuell §akeine §7offenen Reports.")
            ));
            return;
        }

        source.sendMessage(prefix().append(
                Component.text("§7Offene Reports (§b" + open.size() + "§7):")
        ));

        for (ReportEntry r : open) {
            source.sendMessage(Component.text(
                    "§8#§e" + r.id() +
                            " §8| §7Ziel: §c" + r.targetName() +
                            " §8| §7Von: §b" + r.reporterName() +
                            " §8| §7Server: §f" + r.serverName()
            ));
            source.sendMessage(Component.text(
                    "   §7Grund: §f" + r.reason()
            ));
        }
    }

    private void handleReport(CommandSource source, String idArg) {
        long id;
        try {
            id = Long.parseLong(idArg);
        } catch (NumberFormatException e) {
            source.sendMessage(prefix().append(
                    Component.text("§cUngültige ID: §7" + idArg)
            ));
            return;
        }

        if (!(source instanceof Player player)) {
            source.sendMessage(prefix().append(
                    Component.text("§cNur In-Game-Spieler können Reports bearbeiten.")
            ));
            return;
        }

        boolean success = reportService.markReportHandled(id, player.getUsername());

        if (!success) {
            source.sendMessage(prefix().append(
                    Component.text("§cReport mit ID §e#" + id + " §cexistiert nicht oder ist bereits bearbeitet.")
            ));
            return;
        }

        source.sendMessage(prefix().append(
                Component.text("§aReport §e#" + id + " §awurde als bearbeitet markiert.")
        ));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERM_REPORTS);
        // Oder über dein eigenes Permission-System:
        // return permissionService.hasPermission(invocation.source(), PERM_REPORTS);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return List.of("handle");
        }

        return List.of();
    }
}
