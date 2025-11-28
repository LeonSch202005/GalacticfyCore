package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.SessionService;
import org.slf4j.Logger;

public class SessionListener {

    private final SessionService sessions;
    private final Logger logger;

    public SessionListener(SessionService sessions, Logger logger) {
        this.sessions = sessions;
        this.logger = logger;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player p = event.getPlayer();
        String serverName = p.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("Unbekannt");

        sessions.onLogin(p.getUniqueId(), p.getUsername(), serverName);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player p = event.getPlayer();
        sessions.onLogout(p.getUniqueId());
    }
}
