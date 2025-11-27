package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.ReasonPresets;
import de.galacticfy.core.punish.ReasonPresets.Preset;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.stream.Collectors;

public class ReportCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;

    // wer die Reports sehen darf
    private static final String PERM_VIEW_REPORTS = "galacticfy.report.view";

    public ReportCommand(ProxyServer proxy,
                         GalacticfyPermissionService perms) {
        this.proxy = proxy;
        this.perms = perms;
    }

    private Component prefix() {
        return Component.text("§8[§cReport§8] §r");
    }

    private boolean canSeeReports(Player p) {
        if (perms != null) {
            return perms.hasPluginPermission(p, PERM_VIEW_REPORTS);
        }
        return p.hasPermission(PERM_VIEW_REPORTS);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!(src instanceof Player reporter)) {
            src.sendMessage(Component.text("§cDieser Befehl ist nur für Spieler."));
            return;
        }

        if (args.length < 2) {
            reporter.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/report <Spieler> <Preset|Grund...>"
            )));
            return;
        }

        String targetName = args[0];

        // ===========================
        // Grund + Preset
        // ===========================
        String reasonToken = args[1];
        String reason;
        Preset preset = ReasonPresets.find(reasonToken);

        if (preset != null) {
            if (args.length > 2) {
                String extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                reason = preset.display() + " §8(§7" + extra + "§8)";
            } else {
                reason = preset.display();
            }
        } else {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        String reporterName = reporter.getUsername();

        Component msg = Component.text(
                "§8[§cReport§8] §c" + reporterName +
                        " §7hat §e" + targetName +
                        " §7gemeldet: §f" + reason
        );

        // an alle Teamler mit Permission
        proxy.getAllPlayers().forEach(p -> {
            if (canSeeReports(p)) {
                p.sendMessage(msg);
            }
        });

        // in die Konsole
        proxy.getConsoleCommandSource().sendMessage(msg);

        // Feedback für Spieler
        reporter.sendMessage(prefix().append(Component.text(
                "§aDein Report wurde an das Team gesendet."
        )));
    }

    // ===========================
    // TAB-COMPLETE
    // ===========================

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        // /report <Spieler>
        if (args.length == 0 || args.length == 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

            // für Reports reichen Online-Spieler
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // /report <Spieler> <Preset|Grund>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return ReasonPresets.tabComplete(prefix);
        }

        return List.of();
    }
}
