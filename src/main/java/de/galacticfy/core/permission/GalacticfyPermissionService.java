package de.galacticfy.core.permission;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Eigenes Rollen-/Permission-System:
 *  - MariaDB = Rollen / Branding / Gruppen-Permissions
 *  - Velocity hasPermission() kannst du trotzdem weiter nutzen, braucht kein LuckPerms-API.
 */
public class GalacticfyPermissionService {

    private final DatabaseManager db;
    private final Logger logger;

    private final String defaultRoleName = "spieler";

    public GalacticfyPermissionService(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;

        ensureDefaultRole();
    }

    // ---------------------------------------------------
    //  Normale Permissionchecks (JETZT mit Rank-System)
    // ---------------------------------------------------

    /**
     * Permission-Check für beliebige CommandSources.
     *
     * Reihenfolge:
     *  1) Wenn Player → zuerst Rank-Permissions (inkl. "*" & "prefix.*")
     *  2) Fallback auf Velocity source.hasPermission(...)
     */
    public boolean hasPermission(CommandSource source, String permission) {
        if (permission == null || permission.isBlank()) return true;

        // Erst unser eigenes Rank-System
        if (source instanceof Player player) {
            if (hasRankPermission(player, permission)) {
                return true;
            }
        }

        // Dann noch Velocity (Console, andere Plugins etc.)
        return source.hasPermission(permission);
    }

    /**
     * Convenience-Methode nur für Player.
     */
    public boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isBlank()) return true;

        // Erst unser Rank-System
        if (hasRankPermission(player, permission)) {
            return true;
        }

        // Dann noch Velocity-Fallback
        return player.hasPermission(permission);
    }

    // ---------------------------------------------------
    //  ROLLEN AUS DB
    // ---------------------------------------------------

    private void ensureDefaultRole() {
        try (Connection con = db.getConnection()) {

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id FROM gf_roles WHERE name = ?"
            )) {
                ps.setString(1, defaultRoleName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO gf_roles (name, display_name, color_hex, prefix, is_staff, maintenance_bypass, join_priority) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)"
            )) {
                ps.setString(1, defaultRoleName);
                ps.setString(2, "Spieler");
                ps.setString(3, "AAAAAA");
                ps.setString(4, "");
                ps.setBoolean(5, false);
                ps.setBoolean(6, false);
                ps.setInt(7, 0);
                ps.executeUpdate();
            }

            logger.info("GalacticfyCore: Default-Rolle '{}' angelegt.", defaultRoleName);

        } catch (SQLException e) {
            logger.error("Fehler beim Anlegen der Default-Rolle", e);
        }
    }

    public String getDefaultRoleName() {
        return defaultRoleName;
    }

    public boolean createRole(String name, String displayName,
                              String colorHex, String prefix,
                              boolean staff, boolean maintenanceBypass,
                              int joinPriority) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gf_roles (name, display_name, color_hex, prefix, is_staff, maintenance_bypass, join_priority) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)"
             )) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            ps.setString(2, displayName);
            ps.setString(3, colorHex);
            ps.setString(4, prefix);
            ps.setBoolean(5, staff);
            ps.setBoolean(6, maintenanceBypass);
            ps.setInt(7, joinPriority);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Erstellen der Rolle {}", name, e);
            return false;
        }
    }

    public boolean deleteRole(String name) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM gf_roles WHERE name = ?"
             )) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Löschen der Rolle {}", name, e);
            return false;
        }
    }

    public List<String> getAllRoleNames() {
        List<String> roles = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT name FROM gf_roles ORDER BY join_priority DESC, name ASC"
             );
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Rollenliste", e);
        }
        return roles;
    }

    private GalacticfyRole mapRole(ResultSet rs) throws SQLException {
        return new GalacticfyRole(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("display_name"),
                rs.getString("color_hex"),
                rs.getString("prefix"),
                rs.getBoolean("is_staff"),
                rs.getBoolean("maintenance_bypass"),
                rs.getInt("join_priority")
        );
    }

    public GalacticfyRole getRoleByName(String roleName) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM gf_roles WHERE name = ?"
             )) {
            ps.setString(1, roleName.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRole(rs);
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Rolle {}", roleName, e);
        }
        return null;
    }

    public GalacticfyRole getRoleFor(UUID uuid) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT r.* FROM gf_user_roles u JOIN gf_roles r ON u.role_id = r.id WHERE u.uuid = ?"
             )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRole(rs);
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der User-Rolle", e);
        }
        // Fallback: Default-Rolle
        return getRoleByName(defaultRoleName);
    }

    public boolean setRoleFor(UUID uuid, String name, String roleName) {
        try (Connection con = db.getConnection()) {

            GalacticfyRole role = getRoleByName(roleName);
            if (role == null) return false;

            boolean exists;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT uuid FROM gf_user_roles WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE gf_user_roles SET name = ?, role_id = ? WHERE uuid = ?"
                )) {
                    ps.setString(1, name);
                    ps.setInt(2, role.id);
                    ps.setString(3, uuid.toString());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO gf_user_roles (uuid, name, role_id) VALUES (?, ?, ?)"
                )) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setInt(3, role.id);
                    ps.executeUpdate();
                }
            }

            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Setzen der Rolle {} für {}", roleName, uuid, e);
            return false;
        }
    }

    public boolean setRoleToDefault(UUID uuid, String name) {
        return setRoleFor(uuid, name, defaultRoleName);
    }

    // ---------------------------------------------------
    //  Gruppen-Permissions (eigenes System)
    // ---------------------------------------------------

    public boolean addPermissionToRole(String roleName, String permission) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return false;
        if (permission == null || permission.isBlank()) return false;

        String node = permission.toLowerCase(Locale.ROOT);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gf_role_permissions (role_id, permission) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE permission = permission"
             )) {
            ps.setInt(1, role.id);
            ps.setString(2, node);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Hinzufügen der Permission {} zu Rolle {}", node, roleName, e);
            return false;
        }
    }

    public boolean removePermissionFromRole(String roleName, String permission) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return false;
        if (permission == null || permission.isBlank()) return false;

        String node = permission.toLowerCase(Locale.ROOT);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM gf_role_permissions WHERE role_id = ? AND permission = ?"
             )) {
            ps.setInt(1, role.id);
            ps.setString(2, node);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Entfernen der Permission {} von Rolle {}", node, roleName, e);
            return false;
        }
    }

    public List<String> getPermissionsOfRole(String roleName) {
        List<String> list = new ArrayList<>();
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return list;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT permission FROM gf_role_permissions WHERE role_id = ? ORDER BY permission ASC"
             )) {
            ps.setInt(1, role.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("permission"));
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Permissions für Rolle {}", roleName, e);
        }
        return list;
    }

    /**
     * Checkt eine Permission NUR über das Rank-System.
     *
     * Besonderheiten:
     *  - "*" in der Rolle  => ALLE Rechte
     *  - "prefix.*"        => alle Rechte, die mit "prefix." anfangen
     *  - exakte Matches    => z.B. "galacticfy.rank.admin"
     */
    public boolean hasRankPermission(Player player, String permission) {
        if (permission == null || permission.isBlank()) return true;

        GalacticfyRole role = getRoleFor(player.getUniqueId());
        if (role == null) return false;

        List<String> permsList = getPermissionsOfRole(role.name);
        if (permsList.isEmpty()) return false;

        String node = permission.toLowerCase(Locale.ROOT);

        for (String raw : permsList) {
            if (raw == null || raw.isBlank()) continue;
            String p = raw.toLowerCase(Locale.ROOT);

            // Voller Stern: alles erlaubt
            if (p.equals("*")) return true;

            // Exakte Permission
            if (p.equals(node)) return true;

            // Wildcard "prefix.*"
            if (p.endsWith(".*")) {
                String prefix = p.substring(0, p.length() - 2);
                if (!prefix.isEmpty() && node.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------
    //  High-Level Helpers (Staff, Maintenance, Display)
    // ---------------------------------------------------

    public boolean isStaff(Player player) {
        GalacticfyRole role = getRoleFor(player.getUniqueId());
        return role != null && role.staff;
    }

    public boolean hasMaintenanceBypass(Player player) {
        GalacticfyRole role = getRoleFor(player.getUniqueId());
        return role != null && role.maintenanceBypass;
    }

    public Component getDisplayName(Player player) {
        GalacticfyRole role = getRoleFor(player.getUniqueId());
        String name = player.getUsername();

        String prefix = role != null && role.prefix != null ? role.prefix : "";
        String colorHex = role != null && role.colorHex != null ? role.colorHex : "FFFFFF";

        TextColor color;
        try {
            color = TextColor.fromHexString("#" + colorHex);
        } catch (Exception e) {
            color = TextColor.fromHexString("#FFFFFF");
        }

        Component base = Component.text(name).color(color);

        if (!prefix.isEmpty()) {
            return Component.text(prefix).append(Component.space()).append(base);
        }
        return base;
    }
}
