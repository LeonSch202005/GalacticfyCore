package de.galacticfy.core.motd;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.galacticfy.core.service.MaintenanceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Zentrales MOTD-System für das Galacticfy-Netzwerk.
 *
 * - Normale MOTD mit rotierenden Untertiteln
 * - Wartungs-MOTD mit Restzeit & betroffenen Servern
 * - KEIN <center>, nur normale Zeilen
 */
public class GalacticfyMotdProvider {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final MaintenanceService maintenanceService;

    // Rotierende Subtitel für die normale MOTD
    private final List<String> normalSubtitles = List.of(
            "<gray><italic>Im Schatten der Sterne beginnt deine Geschichte…</italic></gray>",
            "<gray><italic>Verbinde Welten. Baue deine Galaxie.</italic></gray>",
            "<gray><italic>Citybuild, Skyblock & mehr – alles in einem Universum.</italic></gray>",
            "<gray><italic>Schließe dich der Crew an und erobere den Kosmos.</italic></gray>"
    );

    public GalacticfyMotdProvider(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing original = event.getPing();
        ServerPing.Builder builder = original.asBuilder();

        if (maintenanceService.isMaintenanceEnabled()) {
            handleMaintenanceMotd(builder);
        } else {
            handleNormalMotd(builder, original);
        }

        event.setPing(builder.build());
    }

    // ---------------------------------------------------------
    // Wartungs-MOTD
    // ---------------------------------------------------------

    private void handleMaintenanceMotd(ServerPing.Builder builder) {
        // Restzeit im schönen Textformat, z.B. "2h 31m"
        String time = maintenanceService.getRemainingTimeFormatted();

        // Set -> List konvertieren
        List<String> maintServers = new ArrayList<>(maintenanceService.getServersInMaintenance());

        String affectedServersLine;
        if (maintServers.isEmpty()) {
            affectedServersLine = "<gray>Teile des Netzwerks befinden sich in Wartung.</gray>";
        } else {
            String joined = String.join(", ", maintServers);
            affectedServersLine = "<gray>Betroffene Server:</gray> <gold>" + joined + "</gold>";
        }

        // HIER wird die Zeit wirklich in die MOTD geschrieben:
        Component motd = mm.deserialize(
                "<gradient:#00E5FF:#7A00FF><bold>✦ Galacticfy Netzwerk ✦</bold></gradient>\n" +
                        "<red><bold>⛔ Wartungsmodus aktiv</bold></red>\n" +
                        "<gray>⏳ Wieder verfügbar in </gray><gold><bold>" + time + "</bold></gold>\n" +
                        "<dark_gray>" + affectedServersLine + "</dark_gray>"
        );


        builder.description(motd);

        // Rechts oben im Server-Listeneintrag
        builder.version(new ServerPing.Version(
                0,
                "Wartung ✘"
        ));
    }

    // ---------------------------------------------------------
    // Normale MOTD
    // ---------------------------------------------------------

    private void handleNormalMotd(ServerPing.Builder builder, ServerPing original) {
        String subtitle = pickRandomSubtitle();

        int online = original.getPlayers().map(p -> p.getOnline()).orElse(0);
        int max = original.getPlayers().map(p -> p.getMax()).orElse(0);

        String playersLine = "<gray>Online:</gray> <aqua>" + online + "</aqua><gray>/</gray><aqua>" + max + "</aqua>";

        Component motd = mm.deserialize(
                "<gradient:#00E5FF:#C800FF><bold>✦ Galacticfy Netzwerk ✦</bold></gradient>\n" +
                        subtitle + "\n" +
                        playersLine
        );

        builder.description(motd);

        builder.version(new ServerPing.Version(
                original.getVersion().getProtocol(),
                "Galacticfy • 1.20.x"
        ));
    }

    private String pickRandomSubtitle() {
        if (normalSubtitles.isEmpty()) {
            return "<gray><italic>Willkommen im Galacticfy Netzwerk.</italic></gray>";
        }
        int idx = ThreadLocalRandom.current().nextInt(normalSubtitles.size());
        return normalSubtitles.get(idx);
    }
}
