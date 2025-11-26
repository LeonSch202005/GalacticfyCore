package de.galacticfy.core.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.UUID;

/**
 * Einheitlicher Tablist-Listener:
 *  - Header/Footer Design
 *  - Tablist-Namen mit Prefix/Suffix aus GalacticfyPermissionService
 *  - KEINE Online-Anzeige / KEIN Servername mehr
 */
public class TablistPrefixListener {

    private final ProxyServer proxy;
    private final GalacticfyPermissionService permissionService;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TablistPrefixListener(ProxyServer proxy,
                                 GalacticfyPermissionService permissionService,
                                 Logger logger) {
        this.proxy = proxy;
        this.permissionService = permissionService;
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        logger.info("[Tablist] PostLogin für {}", player.getUsername());
        refreshAll();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        logger.info("[Tablist] ServerPostConnect für {}", player.getUsername());
        refreshAll();
    }

    /**
     * Geht alle Spieler durch:
     *  - setzt Header/Footer für den Viewer
     *  - aktualisiert für jeden Viewer alle Tab-Einträge (Prefix + Name)
     */
    private void refreshAll() {
        Collection<Player> players = proxy.getAllPlayers();

        for (Player viewer : players) {
            updateHeaderFooter(viewer);

            TabList tabList = viewer.getTabList();

            for (Player target : players) {
                updateEntry(tabList, target);
            }
        }
    }

    private void updateHeaderFooter(Player viewer) {
        // Header OHNE Servername / Online-Anzahl
        Component header = mm.deserialize(
                "<gradient:#00E5FF:#C800FF><bold>✦ Galacticfy Netzwerk ✦</bold></gradient>\n" +
                        "<gray>Zwischen den Sternen beginnt dein Abenteuer.</gray>\n" +
                        "\n" + // Leerzeile
                        "\n"   // noch eine Leerzeile
        );

        // Footer OHNE Online-Anzeige, nur Website/Discord (oder auch leer, wenn du willst)
        Component footer = mm.deserialize(
                "\n" + // Leerzeile über Footer
                        "<yellow>Website:</yellow> <aqua>galacticfy.de</aqua>\n" +
                        "<yellow>Discord:</yellow> <aqua>discord.gg/galacticfy</aqua>\n"
        );

        viewer.getTabList().setHeaderAndFooter(header, footer);
    }

    /**
     * Tablist-Eintrag mit Prefix/Suffix aus deinem Rank-System setzen.
     */
    private void updateEntry(TabList tabList, Player target) {
        UUID uuid = target.getUniqueId();

        // Prefix aus deinem Rank-System (z.B. "&7Spieler", "&4Inhaber")
        Component rankComp = permissionService.getPrefixComponent(target);

        // Fallback, falls eine Rolle KEIN Prefix hat
        if (rankComp == null || rankComp.equals(Component.empty())) {
            rankComp = Component.text("Spieler", NamedTextColor.GRAY);
        }

        // Stern immer drin, dunkelgrau
        Component starComp = Component.text(" ✦ ", NamedTextColor.DARK_GRAY);

        // Name in grau (&7)
        Component nameComp = Component.text(target.getUsername(), NamedTextColor.GRAY);

        // Immer: <Prefix> ✦ <Name>
        Component display = Component.empty()
                .append(rankComp)
                .append(starComp)
                .append(nameComp);

        tabList.getEntry(uuid).ifPresentOrElse(entry -> {
            entry.setDisplayName(display);
        }, () -> {
            TabListEntry entry = TabListEntry.builder()
                    .tabList(tabList)
                    .profile(target.getGameProfile())
                    .displayName(display)
                    .latency(1)
                    .gameMode(0)
                    .listed(true)
                    .build();
            tabList.addEntry(entry);
        });
    }


}
