package pirch.pz.service;

import java.util.Set;

public final class AuthorizationPolicy {
    public static final String PERMISSION_MANAGE = "permissions.manage";
    public static final String PERMISSION_MANAGE_SCOPE = "permissions.manage.scope";
    public static final String ROLE_ASSIGN = "roles.assign";
    public static final String ROLE_ASSIGN_SCOPE = "roles.assign.scope";

    private static final Set<String> OWNER_IMPLIED_PERMISSIONS = Set.of(
        "ownership.use",
        "ownership.manage",
        "ownership.release",
        "ownership.transfer",
        "permissions.manage.scope",
        "roles.assign.scope",
        "ui.node.manage"
    );

    private AuthorizationPolicy() {
    }

    public static boolean isNodeScope(String scopeType, String scopeKey) {
        return normalize(scopeType) != null
            && normalize(scopeKey) != null
            && "node".equalsIgnoreCase(scopeType.trim());
    }

    public static boolean isOwnerImpliedPermission(String permissionKey) {
        String normalized = normalize(permissionKey);
        return normalized != null && OWNER_IMPLIED_PERMISSIONS.contains(normalized);
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
