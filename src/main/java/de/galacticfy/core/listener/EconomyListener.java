package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import de.galacticfy.core.service.EconomyService;

public class EconomyListener {

    private final EconomyService economy;

    public EconomyListener(EconomyService economy) {
        this.economy = economy;
    }

    @Subscribe
    public void onJoin(PlayerChooseInitialServerEvent e) {
        var p = e.getPlayer();

        // Spieleraccount automatisch anlegen (falls nicht vorhanden)
        economy.ensureAccount(p.getUniqueId(), p.getUsername());
    }
}
