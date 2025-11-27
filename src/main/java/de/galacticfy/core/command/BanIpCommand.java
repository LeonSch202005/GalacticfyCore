package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.ReasonPresets;
import de.galacticfy.core.punish.ReasonPresets.Preset;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import de.galacticfy.core.util.DiscordWebhookNotifier;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BanIpCommand implements SimpleCommand {

    private static final String PERM_BANIP = "galacticfy.punish.banip";

    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;
    private final DiscordWebhookNotifier webhook;

    public BanIpCommand(GalacticfyPermissionService perms,
                        PunishmentService punishmentService,
                        DiscordWebhookNotifier webhook) {
        this.perms = perms;
        this.punishmentService = punishmentService;
        this.webhook = webhook;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasBanIpPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_BANIP);
            }
            return p.hasPermission(PERM_BANIP);
        }
        return true; // Konsole darf alles
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasBanIpPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/banip <IP> <Dauer|Preset|perm> [Grund...]"
            )));
            return;
        }

        String ip = args[0];
        String durationOrPreset = args[1].toLowerCase(Locale.ROOT);

        // ===========================================
        // Dauer / Preset auflösen
        // ===========================================
        Long durationMs = null;
        String reason;
        String presetKeyUsed = null;

        Preset preset = ReasonPresets.find(durationOrPreset);
        if (preset != null) {
            durationMs = preset.defaultDurationMs(); // kann null = permanent sein
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

        String staffName = (src instanceof Player p)
                ? p.getUsername()
                : "Konsole";

        // IP-Ban in DB
        Punishment p = punishmentService.banIp(ip, reason, staffName, durationMs);
        if (p == null) {
            src.sendMessage(prefix().append(Component.text("§cKonnte IP-Ban nicht speichern (DB-Fehler oder ungültige IP).")));
            return;
        }

        String durText = (p.expiresAt == null)
                ? "§cPermanent"
                : "§e" + punishmentService.formatRemaining(p);

        // Staff-Feedback
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§aIP §e" + ip + " §awurde gebannt.")));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text("§7Grund: §f" + reason));
        src.sendMessage(Component.text("§7Dauer: " + durText));
        if (presetKeyUsed != null) {
            src.sendMessage(Component.text("§7Preset: §b" + presetKeyUsed));
        }
        src.sendMessage(Component.text("§7Von: §f" + staffName));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));

        // Discord-Webhook
        if (webhook != null && webhook.isEnabled()) {
            webhook.sendBanIp(p);
        }
    }

    // ===========================
    // TAB-COMPLETE
    // ===========================

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (!hasBanIpPermission(invocation.source())) {
            return List.of();
        }

        // /banip <IP> → keine gute Auto-Vervollständigung, bleibt leer
        if (args.length == 1) {
            return List.of();
        }

        // /banip <IP> <Dauer|Preset>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            List<String> base = List.of("10m", "30m", "1h", "6h", "1d", "7d", "30d", "1mo", "1y", "perm");
            List<String> presets = ReasonPresets.tabComplete(prefix);

            new java.util.ArrayList<String>();

            java.util.ArrayList<String> result = new java.util.ArrayList<>();
            for (String s : base) {
                if (s.startsWith(prefix)) result.add(s);
            }
            result.addAll(presets);
            return result;
        }

        return List.of();
    }
}
