package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.PunishDesign;
import de.galacticfy.core.punish.ReasonPresets;
import de.galacticfy.core.punish.ReasonPresets.Preset;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import de.galacticfy.core.util.DiscordWebhookNotifier;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class BanCommand implements SimpleCommand {

    private static final String PERM_BAN = "galacticfy.punish.ban";
    private static final String PERM_PUNISH_PROTECT = "galacticfy.punish.protect";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;
    private final DiscordWebhookNotifier webhook;

    public BanCommand(ProxyServer proxy,
                      GalacticfyPermissionService perms,
                      PunishmentService punishmentService,
                      DiscordWebhookNotifier webhook) {
        this.proxy = proxy;
        this.perms = perms;
        this.punishmentService = punishmentService;
        this.webhook = webhook;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasBanPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_BAN);
            }
            return p.hasPermission(PERM_BAN);
        }
        return true;
    }

    private boolean isProtected(Player target) {
        if (perms != null) {
            return perms.hasPluginPermission(target, PERM_PUNISH_PROTECT);
        }
        return target.hasPermission(PERM_PUNISH_PROTECT);
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasBanPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/ban <Spieler> <Dauer|Preset|perm> [Grund...]"
            )));
            return;
        }

        String targetName = args[0];
        String durationOrPreset = args[1].toLowerCase(Locale.ROOT);

        Player target = proxy.getPlayer(targetName).orElse(null);

        // Selbst-Ban verhindern
        if (src instanceof Player staff && target != null) {
            if (staff.getUniqueId().equals(target.getUniqueId())) {
                staff.sendMessage(prefix().append(Component.text(
                        "§cDu kannst dich nicht selbst bannen."
                )));
                return;
            }
        }

        // Ban-Schutz
        if (target != null && isProtected(target)) {
            src.sendMessage(prefix().append(Component.text(
                    "§cDu kannst diesen Spieler nicht bannen (Team-Schutz aktiv)."
            )));
            return;
        }

        Long durationMs;
        String reason;
        String presetKeyUsed = null;

        Preset preset = ReasonPresets.find(durationOrPreset);
        if (preset != null) {
            durationMs = preset.defaultDurationMs();
            presetKeyUsed = preset.key();

            if (args.length > 2) {
                String extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                reason = preset.display() + " §8(§7" + extra + "§8)";
            } else {
                reason = preset.display();
            }

        } else {
            if (durationOrPreset.equals("perm") || durationOrPreset.equals("permanent")) {
                durationMs = null;
            } else {
                durationMs = punishmentService.parseDuration(durationOrPreset);
                if (durationMs == null) {
                    src.sendMessage(prefix().append(Component.text(
                            "§cUngültige Dauer oder Preset! Beispiele: §b30m§7, §b1h§7, §b7d§7, §bspam§7, §bhackclient§7, §bperm§7"
                    )));
                    return;
                }
            }

            if (args.length > 2) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            } else {
                reason = "Kein Grund angegeben";
            }
        }

        UUID uuid = null;
        String storedName = targetName;
        String ip = null;

        if (target != null) {
            uuid = target.getUniqueId();
            storedName = target.getUsername();

            Object remoteObj = target.getRemoteAddress();
            if (remoteObj instanceof InetSocketAddress isa && isa.getAddress() != null) {
                ip = isa.getAddress().getHostAddress();
            }
        }

        String staffName = (src instanceof Player p) ? p.getUsername() : "Konsole";

        Punishment p = punishmentService.banPlayer(uuid, storedName, ip, reason, staffName, durationMs);
        if (p == null) {
            src.sendMessage(prefix().append(Component.text("§cKonnte Ban nicht speichern (DB-Fehler).")));
            return;
        }

        String durText = (p.expiresAt == null)
                ? "§cPermanent"
                : "§e" + punishmentService.formatRemaining(p);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_BAN));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Spieler: §f" + storedName));
        src.sendMessage(Component.text("§7Grund:  §f" + reason));
        src.sendMessage(Component.text("§7Dauer:  " + durText));
        if (presetKeyUsed != null) {
            src.sendMessage(Component.text("§7Preset: §b" + presetKeyUsed));
        }
        src.sendMessage(Component.text("§7Von:    §f" + staffName));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));

        if (target != null) {
            String remaining = (p.expiresAt == null)
                    ? "§cPermanent"
                    : "§e" + punishmentService.formatRemaining(p);

            Component kickMsg = Component.text(
                    "§c§lGalacticfy §8» §cDu wurdest gebannt.\n" +
                            "§7Grund: §f" + reason + "\n" +
                            "§7Dauer: " + remaining + "\n" +
                            "§7Von: §b" + staffName + "\n" +
                            " \n" +
                            "§7Falls du der Meinung bist, dass dieser Ban §cunberechtigt §7ist,\n" +
                            "§7kannst du einen §bEntbannungsantrag §7auf unserem Discord stellen."
            );
            target.disconnect(kickMsg);
        }

        if (webhook != null && webhook.isEnabled()) {
            webhook.sendBan(p);
        }
    }

    // ===== Sichtbarkeit / Tab wie bei /maintenance =====

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasBanPermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasBanPermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        // /ban <Spieler>
        if (args.length == 1 || args.length == 0) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // /ban <Spieler> <Dauer|Preset>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            List<String> base = Arrays.asList(
                    "10m", "30m", "1h", "6h", "1d", "7d", "30d", "1mo", "1y", "perm"
            );
            List<String> presets = ReasonPresets.tabComplete(prefix);

            List<String> out = new ArrayList<>();
            base.stream().filter(s -> s.startsWith(prefix)).forEach(out::add);
            out.addAll(presets);
            return out;
        }

        return List.of();
    }
}
