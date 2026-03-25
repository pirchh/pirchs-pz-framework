package pirch.pz.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthorizationPolicyTest {
    @Test
    void detectsNodeScopeOnlyWhenBothValuesExist() {
        assertTrue(AuthorizationPolicy.isNodeScope("node", "business:rusty_wrench"));
        assertFalse(AuthorizationPolicy.isNodeScope("node", null));
        assertFalse(AuthorizationPolicy.isNodeScope(null, "business:rusty_wrench"));
        assertFalse(AuthorizationPolicy.isNodeScope("vehicle", "car:spiffo"));
    }

    @Test
    void recognizesOwnerImpliedPermissionKeys() {
        assertTrue(AuthorizationPolicy.isOwnerImpliedPermission("ownership.manage"));
        assertTrue(AuthorizationPolicy.isOwnerImpliedPermission("roles.assign.scope"));
        assertFalse(AuthorizationPolicy.isOwnerImpliedPermission("roles.assign"));
        assertFalse(AuthorizationPolicy.isOwnerImpliedPermission("system.admin"));
    }

    @Test
    void normalizeTrimsBlankValuesToNull() {
        assertNull(AuthorizationPolicy.normalize(null));
        assertNull(AuthorizationPolicy.normalize("   "));
        assertTrue("node".equals(AuthorizationPolicy.normalize(" node ")));
    }
}
