package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.PunishDesign;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.stream.Collectors;

public class UnwarnCommand implements SimpleCommand {

    private static final String PERM_UNWARN = "galacticfy.punish.unwarn";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;

    public UnwarnCommand(ProxyServer proxy,
                         GalacticfyPermissionService perms,
                         PunishmentService punishmentService) {
        this.proxy = proxy;
        this.perms = perms;
        this.punishmentService = punishmentService;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasUnwarnPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_UNWARN);
            }
            return p.hasPermission(PERM_UNWARN);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasUnwarnPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/unwarn <Spieler> [all]"
            )));
            return;
        }

        String targetName = args[0];
        boolean removeAll = args.length >= 2 && args[1].equalsIgnoreCase("all");

        UUID uuid = proxy.getPlayer(targetName)
                .map(Player::getUniqueId)
                .orElse(null);

        String staffName = (src instanceof Player p) ? p.getUsername() : "Konsole";

        if (removeAll) {
            // Du brauchst in PunishmentService:
            // int clearAllWarns(UUID uuid, String name, String staffName)
            int removed = punishmentService.clearAllWarns(uuid, targetName, staffName);

            if (removed <= 0) {
                src.sendMessage(prefix().append(Component.text(
                        "§7Es wurden keine Warns für §e" + targetName + " §7gefunden."
                )));
                return;
            }

            src.sendMessage(Component.text(" "));
            src.sendMessage(Component.text(PunishDesign.BIG_HEADER_UNWARN));
            src.sendMessage(Component.text(" "));
            src.sendMessage(Component.text("§7Spieler: §f" + targetName));
            src.sendMessage(Component.text("§7Entfernt: §e" + removed + " Warn(s)"));
            src.sendMessage(Component.text("§7Von:     §f" + staffName));
            src.sendMessage(Component.text(PunishDesign.LINE));
            src.sendMessage(Component.text(" "));
            return;
        }

        // Nur den letzten Warn entfernen:
        // Punishment clearLastWarn(UUID uuid, String name, String staffName)
        Punishment removed = punishmentService.clearLastWarn(uuid, targetName, staffName);

        if (removed == null) {
            src.sendMessage(prefix().append(Component.text(
                    "§7Es gibt keinen Warn, der entfernt werden kann."
            )));
            return;
        }

        int remaining = punishmentService.countWarns(uuid, targetName);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_UNWARN));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Spieler:   §f" + targetName));
        src.sendMessage(Component.text("§7Entfernt:  §f" + removed.reason));
        src.sendMessage(Component.text("§7Warns jetzt: §e" + remaining));
        src.sendMessage(Component.text("§7Von:       §f" + staffName));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasUnwarnPermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasUnwarnPermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            Set<String> names = new LinkedHashSet<>();
            names.addAll(punishmentService.getAllPunishedNames());
            proxy.getAllPlayers().forEach(p -> names.add(p.getUsername()));
            return names.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            Set<String> names = new LinkedHashSet<>();
            names.addAll(punishmentService.getAllPunishedNames());
            proxy.getAllPlayers().forEach(p -> names.add(p.getUsername()));

            return names.stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("all").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
