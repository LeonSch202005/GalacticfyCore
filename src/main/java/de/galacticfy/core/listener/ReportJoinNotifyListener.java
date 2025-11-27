package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.service.ReportService;
import net.kyori.adventure.text.Component;

public class ReportJoinNotifyListener {

    private static final String PERM_REPORT_VIEW = "galacticfy.report.view";

    private final ReportService reportService;
    private final GalacticfyPermissionService perms;

    public ReportJoinNotifyListener(ReportService reportService,
                                    GalacticfyPermissionService perms) {
        this.reportService = reportService;
        this.perms = perms;
    }

    private boolean canViewReports(Player p) {
        if (perms != null) {
            return perms.hasPluginPermission(p, PERM_REPORT_VIEW);
        }
        return p.hasPermission(PERM_REPORT_VIEW);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (!canViewReports(player)) {
            return;
        }

        int count = reportService.countAllReports();
        if (count <= 0) {
            return;
        }

        player.sendMessage(Component.text(
                "§8[§cReport§8] §7Es gibt aktuell §e" + count +
                        " §7offene Reports. §8(§b/report list§7, §b/report check <Spieler>§8)"
        ));
    }
}
