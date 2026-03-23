package pirch.pz.service;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pzloader.util.LoaderLog;

public final class AuthSelfTestService {
    private static volatile Map<String, Object> lastStatus = new LinkedHashMap<>();
    private static volatile boolean hasRunThisSession = false;
    private static volatile PlayerIdentity lastIdentity = null;
    private static volatile Integer lastAccountId = null;

    private AuthSelfTestService() {
    }

    public static synchronized void reset(String reason) {
        hasRunThisSession = false;
        lastIdentity = null;
        lastAccountId = null;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", false);
        status.put("state", "reset");
        status.put("reason", reason);
        lastStatus = status;

        LoaderLog.info("[PZLIFE][AUTH][selftest] reset. reason=" + reason);
    }

    public static synchronized void onLocalAccountResolved(PlayerIdentity identity, int accountId) {
        lastIdentity = identity;
        lastAccountId = accountId;
    }

    public static synchronized void runAfterLocalResolution(PlayerIdentity identity, int accountId) {
        lastIdentity = identity;
        lastAccountId = accountId;

        if (!PzRuntimeConfig.isAuthSelfTestEnabled()) {
            LoaderLog.info("[PZLIFE][AUTH][selftest] skipped because auth.selftest.enabled=false");
            return;
        }

        if (hasRunThisSession) {
            LoaderLog.info("[PZLIFE][AUTH][selftest] already ran for this session; skipping duplicate trigger");
            return;
        }

        hasRunThisSession = true;
        LoaderLog.info(
            "[PZLIFE][AUTH][selftest] starting after local resolution. accountId=" + accountId
                + ", accountExternalId=" + (identity != null ? identity.getAccountExternalId() : null)
        );

        lastStatus = runInternal(identity, accountId);

        if (Boolean.TRUE.equals(lastStatus.get("ok"))) {
            LoaderLog.info("[PZLIFE][AUTH][selftest] ok: " + lastStatus);
        } else {
            LoaderLog.error("[PZLIFE][AUTH][selftest] failed: " + lastStatus);
        }
    }

    public static synchronized Map<String, Object> runNow() {
        if (!PzRuntimeConfig.isAuthSelfTestEnabled()) {
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("ok", false);
            status.put("state", "skipped");
            status.put("reason", "auth.selftest.enabled=false");
            lastStatus = status;
            LoaderLog.info("[PZLIFE][AUTH][selftest] manual run skipped because auth.selftest.enabled=false");
            return new LinkedHashMap<>(lastStatus);
        }

        if (lastIdentity == null || lastAccountId == null) {
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("ok", false);
            status.put("state", "skipped");
            status.put("reason", "no local account has been resolved yet");
            lastStatus = status;
            LoaderLog.info("[PZLIFE][AUTH][selftest] manual run skipped because no local account is resolved yet");
            return new LinkedHashMap<>(lastStatus);
        }

        LoaderLog.info(
            "[PZLIFE][AUTH][selftest] manual run requested. accountId=" + lastAccountId
                + ", accountExternalId=" + lastIdentity.getAccountExternalId()
        );

        hasRunThisSession = true;
        lastStatus = runInternal(lastIdentity, lastAccountId);

        if (Boolean.TRUE.equals(lastStatus.get("ok"))) {
            LoaderLog.info("[PZLIFE][AUTH][selftest] manual run ok: " + lastStatus);
        } else {
            LoaderLog.error("[PZLIFE][AUTH][selftest] manual run failed: " + lastStatus);
        }

        return new LinkedHashMap<>(lastStatus);
    }

    public static synchronized Map<String, Object> getStatus() {
        return new LinkedHashMap<>(lastStatus);
    }

    private static Map<String, Object> runInternal(PlayerIdentity identity, int accountId) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("accountId", accountId);
        status.put("accountExternalId", identity != null ? identity.getAccountExternalId() : null);

        try {
            String nodeKey = PzRuntimeConfig.getAuthSelfTestNodeKey();
            String nodeType = PzRuntimeConfig.getAuthSelfTestNodeType();
            String permissionKey = PzRuntimeConfig.getAuthSelfTestPermissionKey();
            String scopeType = PzRuntimeConfig.getAuthSelfTestScopeType();
            String scopeKey = PzRuntimeConfig.getAuthSelfTestScopeKey();

            status.put("nodeKey", nodeKey);
            status.put("nodeType", nodeType);
            status.put("permissionKey", permissionKey);
            status.put("scopeType", scopeType);
            status.put("scopeKey", scopeKey);

            LoaderLog.info(
                "[PZLIFE][AUTH][selftest] claiming test node. nodeType=" + nodeType
                    + ", nodeKey=" + nodeKey
            );
            Map<String, Object> claimResult = OwnershipService.claimNode(identity, nodeKey, nodeType);
            status.put("claim", claimResult);

            LoaderLog.info("[PZLIFE][AUTH][selftest] checking ownership");
            boolean owner = OwnershipService.isOwner(identity, nodeKey);
            status.put("isOwner", owner);

            LoaderLog.info(
                "[PZLIFE][AUTH][selftest] explaining permission. permissionKey=" + permissionKey
                    + ", scopeType=" + scopeType
                    + ", scopeKey=" + scopeKey
            );
            Map<String, Object> explainResult = PermissionService.explain(identity, permissionKey, scopeType, scopeKey);
            status.put("explain", explainResult);

            status.put("ok", true);
            status.put("state", "ok");
            return status;
        } catch (Exception e) {
            status.put("ok", false);
            status.put("state", "failed");
            status.put("error", e.getMessage());
            LoaderLog.error("[PZLIFE][AUTH][selftest] exception during runInternal: " + e.getMessage());
            return status;
        }
    }
}