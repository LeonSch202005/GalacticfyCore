package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import org.slf4j.Logger;

/**
 * Hängt dein eigenes Rank-/Permission-System in Velocity ein.
 *
 * Ergebnis:
 *  - Alle hasPermission()-Checks laufen über GalacticfyPermissionService.hasRankPermission(...)
 *  - Wenn eine Rolle "*" hat, ist der Spieler effektiv OP (TRUE für alles).
 */
public class PermissionsSetupListener {

    private final GalacticfyPermissionService permissionService;
    private final Logger logger;

    public PermissionsSetupListener(GalacticfyPermissionService permissionService, Logger logger) {
        this.permissionService = permissionService;
        this.logger = logger;
    }

    @Subscribe
    public void onPermissionsSetup(PermissionsSetupEvent event) {
        PermissionSubject subject = event.getSubject();
        final PermissionProvider baseProvider = event.getProvider(); // bisheriger Provider (Velocity/LuckPerms/…)

        // Unser eigener Provider
        PermissionProvider customProvider = new PermissionProvider() {
            @Override
            public PermissionFunction createFunction(PermissionSubject s) {

                // === Spieler → Dein Rank-System ===
                if (s instanceof Player player) {
                    return permission -> {
                        // 1) Galacticfy-Rank-System
                        boolean allowed = permissionService.hasRankPermission(player, permission);
                        if (allowed) {
                            return Tristate.TRUE;  // "*" aus DB = ALLES
                        }

                        // 2) Fallback: ursprünglicher Provider (falls du ihn noch benutzen willst)
                        PermissionFunction original = baseProvider.createFunction(s);
                        return original.getPermissionValue(permission);
                    };
                }

                // === Konsole → alles erlaubt ===
                if (s instanceof ConsoleCommandSource) {
                    return permission -> Tristate.TRUE;
                }

                // === Andere Subjects → Standard-Verhalten beibehalten ===
                PermissionFunction original = baseProvider.createFunction(s);
                return permission -> original.getPermissionValue(permission);
            }
        };

        event.setProvider(customProvider);

        logger.info(
                "Permissions-Provider für {} wurde auf Galacticfy gesetzt (Subject-Typ: {}).",
                subject, subject.getClass().getSimpleName()
        );
    }
}
