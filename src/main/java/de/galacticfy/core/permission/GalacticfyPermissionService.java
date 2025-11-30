package de.galacticfy.core.permission;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import javax.management.relation.Role;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Eigenes Rollen-/Permission-System:
 *  - MariaDB = Rollen / Branding / Gruppen-Permissions / Inheritance / Expire
 *  - Caching: Rollen, Permissions, User-Rollen
 *
 * Features:
 *  - Prefix + Suffix
 *  - Permission-Wildcards: "*", "foo.*"
 *  - Role-Inherit (Rollen erben andere Rollen)
 *  - Expiring Ranks (expires_at)
 */
public class GalacticfyPermissionService {

    private final DatabaseManager db;
    private final Logger logger;

    private final String defaultRoleName = "spieler";

    private final Map<String, GalacticfyRole> roleByName = new ConcurrentHashMap<>();
    private final Map<Integer, GalacticfyRole> roleById = new ConcurrentHashMap<>();

    private final Map<Integer, Set<String>> permissionsByRoleId = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> parentsByRoleId = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> effectivePermissionsCache = new ConcurrentHashMap<>();

    private final Map<UUID, CachedUserRole> userRoleCache = new ConcurrentHashMap<>();


    // Cache: User → Rolle + Expire
    private static class CachedUserRole {
        final GalacticfyRole role;
        final String name;
        final Long expiresAtMillis;

        CachedUserRole(GalacticfyRole role, String name, Long expiresAtMillis) {
            this.role = role;
            this.name = name;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    public GalacticfyPermissionService(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;

        ensureDefaultRole();
        reloadAllRoles();
        reloadAllRolePermissions();
        reloadAllInheritance();
    }

    // ---------------------------------------------------
    //  Normale Permissionchecks über Velocity
    // ---------------------------------------------------

    public boolean hasPermission(CommandSource source, String permission) {
        if (permission == null || permission.isBlank()) return true;
        return source.hasPermission(permission);
    }

    public boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isBlank()) return true;
        return player.hasPermission(permission);
    }

    // ---------------------------------------------------
    //  ROLLEN AUS DB + CACHE
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
                    "INSERT INTO gf_roles (name, display_name, color_hex, prefix, suffix, is_staff, maintenance_bypass, join_priority) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            )) {
                ps.setString(1, defaultRoleName);
                ps.setString(2, "Spieler");
                ps.setString(3, "AAAAAA");
                ps.setString(4, "");
                ps.setString(5, "");
                ps.setBoolean(6, false);
                ps.setBoolean(7, false);
                ps.setInt(8, 0);
                ps.executeUpdate();
            }

            logger.info("GalacticfyCore: Default-Rolle '{}' angelegt.", defaultRoleName);

        } catch (SQLException e) {
            logger.error("Fehler beim Anlegen der Default-Rolle", e);
        }
    }

    private GalacticfyRole mapRole(ResultSet rs) throws SQLException {
        return new GalacticfyRole(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("display_name"),
                rs.getString("color_hex"),
                rs.getString("prefix"),
                rs.getString("suffix"),
                rs.getBoolean("is_staff"),
                rs.getBoolean("maintenance_bypass"),
                rs.getInt("join_priority")
        );
    }

    private void cacheRole(GalacticfyRole role) {
        if (role == null) return;
        roleByName.put(role.name.toLowerCase(Locale.ROOT), role);
        roleById.put(role.id, role);
    }

    public void reloadAllRoles() {
        roleByName.clear();
        roleById.clear();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM gf_roles");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                GalacticfyRole role = mapRole(rs);
                cacheRole(role);
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden aller Rollen", e);
        }
        effectivePermissionsCache.clear();
    }

    public String getDefaultRoleName() {
        return defaultRoleName;
    }

    public GalacticfyRole getRoleByName(String roleName) {
        if (roleName == null || roleName.isBlank()) return null;
        String key = roleName.toLowerCase(Locale.ROOT);

        GalacticfyRole cached = roleByName.get(key);
        if (cached != null) return cached;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM gf_roles WHERE name = ?"
             )) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GalacticfyRole role = mapRole(rs);
                    cacheRole(role);
                    return role;
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Rolle {}", roleName, e);
        }
        return null;
    }

    private GalacticfyRole getRoleById(int id) {
        GalacticfyRole cached = roleById.get(id);
        if (cached != null) return cached;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM gf_roles WHERE id = ?"
             )) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GalacticfyRole role = mapRole(rs);
                    cacheRole(role);
                    return role;
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Rolle mit ID {}", id, e);
        }
        return null;
    }

    public boolean createRole(String name, String displayName,
                              String colorHex, String prefix, String suffix,
                              boolean staff, boolean maintenanceBypass,
                              int joinPriority) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gf_roles (name, display_name, color_hex, prefix, suffix, is_staff, maintenance_bypass, join_priority) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS
             )) {
            String key = name.toLowerCase(Locale.ROOT);
            ps.setString(1, key);
            ps.setString(2, displayName);
            ps.setString(3, colorHex);
            ps.setString(4, prefix);
            ps.setString(5, suffix);
            ps.setBoolean(6, staff);
            ps.setBoolean(7, maintenanceBypass);
            ps.setInt(8, joinPriority);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    GalacticfyRole role = new GalacticfyRole(
                            id, key, displayName, colorHex, prefix, suffix, staff, maintenanceBypass, joinPriority
                    );
                    cacheRole(role);
                    effectivePermissionsCache.clear();
                }
            }
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Erstellen der Rolle {}", name, e);
            return false;
        }
    }

    public boolean deleteRole(String name) {
        GalacticfyRole role = getRoleByName(name);
        if (role == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM gf_roles WHERE id = ?"
             )) {
            ps.setInt(1, role.id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                roleByName.remove(role.name.toLowerCase(Locale.ROOT));
                roleById.remove(role.id);
                permissionsByRoleId.remove(role.id);
                parentsByRoleId.remove(role.id);
                effectivePermissionsCache.clear();
            }
            return ok;
        } catch (SQLException e) {
            logger.error("Fehler beim Löschen der Rolle {}", name, e);
            return false;
        }
    }

    public boolean updateRolePrefix(String roleName, String newPrefix) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_roles SET prefix = ? WHERE id = ?"
             )) {
            ps.setString(1, newPrefix);
            ps.setInt(2, role.id);
            int updated = ps.executeUpdate();
            if (updated == 0) return false;

            GalacticfyRole updatedRole = new GalacticfyRole(
                    role.id,
                    role.name,
                    role.displayName,
                    role.colorHex,
                    newPrefix,
                    role.suffix,
                    role.staff,
                    role.maintenanceBypass,
                    role.joinPriority
            );
            cacheRole(updatedRole);
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Aktualisieren des Prefix für Rolle {}", roleName, e);
            return false;
        }
    }

    public boolean updateRoleSuffix(String roleName, String newSuffix) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_roles SET suffix = ? WHERE id = ?"
             )) {
            ps.setString(1, newSuffix);
            ps.setInt(2, role.id);
            int updated = ps.executeUpdate();
            if (updated == 0) return false;

            GalacticfyRole updatedRole = new GalacticfyRole(
                    role.id,
                    role.name,
                    role.displayName,
                    role.colorHex,
                    role.prefix,
                    newSuffix,
                    role.staff,
                    role.maintenanceBypass,
                    role.joinPriority
            );
            cacheRole(updatedRole);
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Aktualisieren des Suffix für Rolle {}", roleName, e);
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

    // ---------------------------------------------------
    //  USER → ROLLE (+ Expire) + Cache
    // ---------------------------------------------------

    public GalacticfyRole getRoleFor(UUID uuid) {
        long now = System.currentTimeMillis();

        CachedUserRole cached = userRoleCache.get(uuid);
        if (cached != null) {
            if (cached.expiresAtMillis != null && cached.expiresAtMillis <= now) {
                logger.info("Rang für {} ist abgelaufen, setze auf Default.", uuid);
                setRoleToDefault(uuid, cached.name != null ? cached.name : "Unknown");
                userRoleCache.remove(uuid);
                return getRoleByName(defaultRoleName);
            }
            if (cached.role != null) {
                return cached.role;
            }
        }

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT u.name, u.role_id, u.expires_at, r.* " +
                             "FROM gf_user_roles u JOIN gf_roles r ON u.role_id = r.id " +
                             "WHERE u.uuid = ?"
             )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    Timestamp ts = rs.getTimestamp("expires_at");
                    Long expiresAtMillis = ts != null ? ts.toInstant().toEpochMilli() : null;

                    GalacticfyRole role = mapRole(rs);

                    if (expiresAtMillis != null && expiresAtMillis <= now) {
                        logger.info("Rang {} für {} ist abgelaufen (DB), setze auf Default.", role.name, uuid);
                        setRoleToDefault(uuid, name != null ? name : "Unknown");
                        userRoleCache.remove(uuid);
                        return getRoleByName(defaultRoleName);
                    }

                    cacheRole(role);
                    userRoleCache.put(uuid, new CachedUserRole(role, name, expiresAtMillis));
                    return role;
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der User-Rolle", e);
        }

        GalacticfyRole def = getRoleByName(defaultRoleName);
        if (def == null) {
            ensureDefaultRole();
            def = getRoleByName(defaultRoleName);
        }
        return def;
    }

    public boolean setRoleFor(UUID uuid, String name, String roleName) {
        return setRoleFor(uuid, name, roleName, null);
    }

    /**
     * Setzt Rolle + optionales Ablaufdatum (Millis seit Epoch).
     * expiresAtMillis == null → kein Ablauf.
     */
    public boolean setRoleFor(UUID uuid, String name, String roleName, Long expiresAtMillis) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return false;

        try (Connection con = db.getConnection()) {

            boolean exists;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT uuid FROM gf_user_roles WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            Timestamp ts = (expiresAtMillis != null)
                    ? Timestamp.from(Instant.ofEpochMilli(expiresAtMillis))
                    : null;

            if (exists) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE gf_user_roles SET name = ?, role_id = ?, expires_at = ? WHERE uuid = ?"
                )) {
                    ps.setString(1, name);
                    ps.setInt(2, role.id);
                    if (ts != null) {
                        ps.setTimestamp(3, ts);
                    } else {
                        ps.setNull(3, Types.TIMESTAMP);
                    }
                    ps.setString(4, uuid.toString());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO gf_user_roles (uuid, name, role_id, expires_at) VALUES (?, ?, ?, ?)"
                )) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setInt(3, role.id);
                    if (ts != null) {
                        ps.setTimestamp(4, ts);
                    } else {
                        ps.setNull(4, Types.TIMESTAMP);
                    }
                    ps.executeUpdate();
                }
            }

            userRoleCache.put(uuid, new CachedUserRole(role, name, expiresAtMillis));
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Setzen der Rolle {} für {}", roleName, uuid, e);
            return false;
        }
    }

    public boolean setRoleToDefault(UUID uuid, String name) {
        return setRoleFor(uuid, name, defaultRoleName, null);
    }

    /**
     * Komfort: Rang für X Dauer setzen (Millis).
     */
    public boolean setRoleForDuration(UUID uuid, String name, String roleName, long durationMs) {
        long expiresAtMillis = System.currentTimeMillis() + durationMs;
        return setRoleFor(uuid, name, roleName, expiresAtMillis);
    }

    /**
     * Sucht die UUID eines Spielers anhand des zuletzt gespeicherten Namens.
     * Wird für Offline-Support von /rank user benutzt.
     */
    public UUID findUuidByName(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.toLowerCase(Locale.ROOT);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT uuid, name FROM gf_user_roles WHERE LOWER(name) = ? LIMIT 1"
             )) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    try {
                        return UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Ungültige UUID '{}' in gf_user_roles für name={}", uuidStr, name);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der UUID für Name {}", name, e);
        }
        return null;
    }

    // ---------------------------------------------------
    //  Gruppen-Permissions (eigenes System) + Cache
    // ---------------------------------------------------

    public void reloadAllRolePermissions() {
        permissionsByRoleId.clear();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT role_id, permission FROM gf_role_permissions"
             );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int roleId = rs.getInt("role_id");
                String perm = rs.getString("permission");
                if (perm == null || perm.isBlank()) continue;
                String node = perm.toLowerCase(Locale.ROOT);

                permissionsByRoleId
                        .computeIfAbsent(roleId, k -> ConcurrentHashMap.newKeySet())
                        .add(node);
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Rollen-Permissions", e);
        }
        effectivePermissionsCache.clear();
    }

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

            permissionsByRoleId
                    .computeIfAbsent(role.id, k -> ConcurrentHashMap.newKeySet())
                    .add(node);

            effectivePermissionsCache.clear();
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
            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                Set<String> set = permissionsByRoleId.get(role.id);
                if (set != null) {
                    set.remove(node);
                }
                effectivePermissionsCache.clear();
            }

            return ok;
        } catch (SQLException e) {
            logger.error("Fehler beim Entfernen der Permission {} von Rolle {}", node, roleName, e);
            return false;
        }
    }

    // ---------------------------------------------------
    //  Zentrale Permission-Abfrage für DEIN Plugin
    // ---------------------------------------------------
    public boolean hasPluginPermission(CommandSource source, String permission) {
        if (permission == null || permission.isBlank()) return true;

        if (!(source instanceof Player player)) {
            return true;
        }

        if (hasRankPermission(player, permission)) {
            return true;
        }

        return player.hasPermission(permission);
    }

    public List<String> getPermissionsOfRole(String roleName) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return List.of();

        Set<String> set = permissionsByRoleId.get(role.id);
        if (set == null || set.isEmpty()) return List.of();

        List<String> list = new ArrayList<>(set);
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    // ---------------------------------------------------
    //  ROLE-INHERIT (Vererbung)
    // ---------------------------------------------------

    public void reloadAllInheritance() {
        parentsByRoleId.clear();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT role_id, parent_role_id FROM gf_role_inherits"
             );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int roleId = rs.getInt("role_id");
                int parentId = rs.getInt("parent_role_id");
                parentsByRoleId
                        .computeIfAbsent(roleId, k -> ConcurrentHashMap.newKeySet())
                        .add(parentId);
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Rollen-Vererbung", e);
        }
        effectivePermissionsCache.clear();
    }

    public boolean addInheritedRole(String roleName, String parentRoleName) {
        GalacticfyRole role = getRoleByName(roleName);
        GalacticfyRole parent = getRoleByName(parentRoleName);
        if (role == null || parent == null) return false;

        if (role.id == parent.id) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gf_role_inherits (role_id, parent_role_id) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE role_id = role_id"
             )) {
            ps.setInt(1, role.id);
            ps.setInt(2, parent.id);
            ps.executeUpdate();

            parentsByRoleId
                    .computeIfAbsent(role.id, k -> ConcurrentHashMap.newKeySet())
                    .add(parent.id);

            effectivePermissionsCache.clear();
            return true;
        } catch (SQLException e) {
            logger.error("Fehler beim Hinzufügen von Inherit {} -> {}", roleName, parentRoleName, e);
            return false;
        }
    }

    public boolean removeInheritedRole(String roleName, String parentRoleName) {
        GalacticfyRole role = getRoleByName(roleName);
        GalacticfyRole parent = getRoleByName(parentRoleName);
        if (role == null || parent == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM gf_role_inherits WHERE role_id = ? AND parent_role_id = ?"
             )) {
            ps.setInt(1, role.id);
            ps.setInt(2, parent.id);
            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                Set<Integer> set = parentsByRoleId.get(role.id);
                if (set != null) {
                    set.remove(parent.id);
                }
                effectivePermissionsCache.clear();
            }

            return ok;
        } catch (SQLException e) {
            logger.error("Fehler beim Entfernen von Inherit {} -> {}", roleName, parentRoleName, e);
            return false;
        }
    }

    public List<String> getParentsOfRole(String roleName) {
        GalacticfyRole role = getRoleByName(roleName);
        if (role == null) return List.of();

        Set<Integer> parentIds = parentsByRoleId.getOrDefault(role.id, Set.of());
        List<String> list = new ArrayList<>();

        for (int parentId : parentIds) {
            GalacticfyRole parent = getRoleById(parentId);
            if (parent != null) {
                list.add(parent.name);
            }
        }
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    private Set<String> computeEffectivePermissions(GalacticfyRole role, Set<Integer> visited) {
        if (role == null) return Set.of();

        if (!visited.add(role.id)) {
            return Set.of();
        }

        Set<String> result = new HashSet<>();

        Set<String> own = permissionsByRoleId.get(role.id);
        if (own != null) {
            result.addAll(own);
        }

        Set<Integer> parents = parentsByRoleId.get(role.id);
        if (parents != null) {
            for (int parentId : parents) {
                GalacticfyRole parent = getRoleById(parentId);
                if (parent == null) continue;
                result.addAll(computeEffectivePermissions(parent, visited));
            }
        }

        return result;
    }

    private Set<String> getEffectivePermissionsForRole(GalacticfyRole role) {
        if (role == null) return Set.of();

        Set<String> cached = effectivePermissionsCache.get(role.id);
        if (cached != null) return cached;

        Set<String> perms = computeEffectivePermissions(role, new HashSet<>());
        Set<String> unmodifiable = Collections.unmodifiableSet(perms);
        effectivePermissionsCache.put(role.id, unmodifiable);
        return unmodifiable;
    }

    public boolean hasRankPermission(Player player, String permission) {
        if (permission == null || permission.isBlank()) return true;

        GalacticfyRole role = getRoleFor(player.getUniqueId());
        if (role == null) return false;

        Set<String> permsList = getEffectivePermissionsForRole(role);
        if (permsList.isEmpty()) return false;

        String node = permission.toLowerCase(Locale.ROOT);

        for (String raw : permsList) {
            if (raw == null || raw.isBlank()) continue;
            String p = raw.toLowerCase(Locale.ROOT);

            if (p.equals("*")) return true;

            if (p.equals(node)) return true;

            if (p.endsWith(".*")) {
                String prefix = p.substring(0, p.length() - 2);
                if (!prefix.isEmpty() && node.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Lädt Rollen, Permissions, Inheritance und den User-Cache komplett neu.
     * Wird z.B. von /rank reload aufgerufen.
     */
    // ---------------------------------------------------
//  Reload für /rank reload
// ---------------------------------------------------
    public void reloadAllCaches() {
        logger.info("GalacticfyPermissionService: Starte /rank reload ...");

        // alle Caches leeren
        roleByName.clear();
        roleById.clear();
        permissionsByRoleId.clear();
        parentsByRoleId.clear();
        effectivePermissionsCache.clear();
        userRoleCache.clear();

        // Default-Rolle sichern
        ensureDefaultRole();

        // alles neu aus der DB laden
        reloadAllRoles();            // lädt gf_roles in roleByName/roleById
        reloadAllRolePermissions();  // lädt gf_role_permissions
        reloadAllInheritance();      // lädt gf_role_inherits

        logger.info("GalacticfyPermissionService: Reload abgeschlossen.");
    }
    public boolean hasConsoleOrPluginPerm(CommandSource src, String permission) {
        // Konsole / Proxy-Intern: immer erlaubt
        if (!(src instanceof Player)) {
            return true;
        }

        // Spieler → ganz normal dein Plugin-Permissionsystem benutzen
        return hasPluginPermission(src, permission);
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

        String prefixRaw = (role != null && role.prefix != null) ? role.prefix : "";
        String suffixRaw = (role != null && role.suffix != null) ? role.suffix : "";
        String colorHex = (role != null && role.colorHex != null) ? role.colorHex : "FFFFFF";

        // & → § für Legacy-Farbcodes
        String prefixLegacy = prefixRaw.replace('&', '§');
        String suffixLegacy = suffixRaw.replace('&', '§');

        TextColor nameColor;
        try {
            nameColor = TextColor.fromHexString("#" + colorHex);
        } catch (Exception e) {
            nameColor = TextColor.color(0xFFFFFF);
        }

        // Name in Rollenfarbe
        Component nameComp = Component.text(name).color(nameColor);

        // Prefix (mit Farben aus &-Codes)
        Component prefixComp = prefixLegacy.isBlank()
                ? Component.empty()
                : LegacyComponentSerializer.legacySection().deserialize(prefixLegacy + " ");

        // Suffix (optional)
        Component suffixComp = suffixLegacy.isBlank()
                ? Component.empty()
                : Component.text(" ").append(
                LegacyComponentSerializer.legacySection().deserialize(suffixLegacy)
        );

        return Component.empty()
                .append(prefixComp)
                .append(nameComp)
                .append(suffixComp);
    }

    public Component getPrefixComponent(Player player) {
        // du hast diese Methode bereits
        GalacticfyRole role = getRoleFor(player.getUniqueId());

        if (role == null || role.prefix == null || role.prefix.isBlank()) {
            return Component.empty();
        }

        // & → § konvertieren, dann als Legacy-Text parsen
        String legacy = role.prefix.replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }
    /**
     * Prüft die Permission eines OFFLINE Spielers anhand der gespeicherten Rollen.
     */
    public boolean hasOfflinePlayerPermission(String name, String permission) {
        if (name == null || permission == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT r.permissions FROM gf_roles r " +
                             "JOIN gf_user_roles ur ON ur.role_name = r.name " +
                             "WHERE LOWER(ur.username) = ?"
             )) {

            ps.setString(1, name.toLowerCase(Locale.ROOT));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String permsString = rs.getString("permissions");
                    if (permsString == null) continue;

                    String[] perms = permsString.split(";");
                    for (String p : perms) {
                        if (p.equalsIgnoreCase(permission)) {
                            return true;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Offline-Permission-Check für {}", name, e);
        }

        return false;
    }



}
