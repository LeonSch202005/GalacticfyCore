package de.galacticfy.core.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class PunishmentLoginListener {

    private final PunishmentService punishmentService;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PunishmentLoginListener(PunishmentService punishmentService, Logger logger) {
        this.punishmentService = punishmentService;
        this.logger = logger;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {

        // Wenn der Login sowieso schon geblockt ist (z.B. Maintenance), nichts tun
        if (!event.getResult().isAllowed()) {
            return;
        }

        Player player = event.getPlayer();

        // ============================
        // IP-Adresse sicher auslesen
        // ============================
        String ip = null;
        InetSocketAddress address = player.getRemoteAddress();
        if (address != null && address.getAddress() != null) {
            ip = address.getAddress().getHostAddress();
        }

        // ============================
        // Aktiven Ban prüfen
        // ============================
        Punishment ban = punishmentService.getActiveBan(player.getUniqueId(), ip);
        if (ban == null) {
            return; // kein Ban -> Spieler darf joinen
        }

        String remaining = punishmentService.formatRemaining(ban);

        Component kickMessage = mm.deserialize(
                "\n" +
                        "<gradient:#FF0000:#7A00FF><bold>Galacticfy</bold></gradient> <gray>|</gray> <red><bold>Du bist gebannt</bold></red>\n" +
                        "<gray>Grund:</gray> <yellow>" + escape(ban.reason) + "</yellow>\n" +
                        "<gray>Gebannt von:</gray> <aqua>" + escape(ban.staff) + "</aqua>\n" +
                        "<gray>Dauer:</gray> <gold>" + escape(remaining) + "</gold>\n" +
                        "\n" +
                        "<yellow>Weitere Infos:</yellow>\n" +
                        "<gray>• Website:</gray> <aqua>https://galacticfy.de</aqua>\n" +
                        "<gray>• Discord:</gray> <aqua>discord.gg/galacticfy</aqua>"
        );

        event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));

        logger.info("Spieler {} wurde beim Login wegen Ban (id={}) geblockt.",
                player.getUsername(), ban.id);
    }

    // MiniMessage-Escape für Grund / Staff / Dauer
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("<", "\\<").replace(">", "\\>");
    }
}
