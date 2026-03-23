package pirch.pz.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthorizationPolicyTest {
    @Test
    void ownerImpliedPermissionsStayExplicit() {
        assertTrue(AuthorizationPolicy.isOwnerImpliedPermission("ownership.manage"));
        assertTrue(AuthorizationPolicy.isOwnerImpliedPermission("permissions.manage.scope"));
        assertTrue(AuthorizationPolicy.isOwnerImpliedPermission("roles.assign.scope"));
        assertFalse(AuthorizationPolicy.isOwnerImpliedPermission("business.manage"));
    }

    @Test
    void nodeScopeRequiresBothTypeAndKey() {
        assertTrue(AuthorizationPolicy.isNodeScope("node", "vehicle:car-001"));
        assertFalse(AuthorizationPolicy.isNodeScope("node", null));
        assertFalse(AuthorizationPolicy.isNodeScope(null, "vehicle:car-001"));
        assertFalse(AuthorizationPolicy.isNodeScope("vehicle", "vehicle:car-001"));
    }
}
