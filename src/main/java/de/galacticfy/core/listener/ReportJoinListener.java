package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.service.ReportService;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class ReportJoinListener {

    private static final String PERM_REPORT_VIEW = "galacticfy.report.view";

    private final ReportService reportService;
    private final GalacticfyPermissionService perms;
    private final Logger logger;

    public ReportJoinListener(ReportService reportService,
                              GalacticfyPermissionService perms,
                              Logger logger) {
        this.reportService = reportService;
        this.perms = perms;
        this.logger = logger;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        boolean canView;
        if (perms != null) {
            canView = perms.hasPluginPermission(player, PERM_REPORT_VIEW);
        } else {
            canView = player.hasPermission(PERM_REPORT_VIEW);
        }

        if (!canView) {
            return; // kein Team, keine Info
        }

        int open = reportService.countOpenReports();

        if (open <= 0) {
            // wenn du auch bei 0 eine Info willst, hier stattdessen Nachricht schicken
            return;
        }

        player.sendMessage(prefix().append(Component.text(
                "§7Es gibt aktuell §e" + open + " §7offene Reports."
        )));
    }
}

