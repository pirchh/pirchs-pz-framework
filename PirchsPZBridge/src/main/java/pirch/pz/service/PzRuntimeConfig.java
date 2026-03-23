package pirch.pz.service;

import java.io.InputStream;
import java.util.Properties;

public final class PzRuntimeConfig {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream inputStream = PzRuntimeConfig.class.getClassLoader().getResourceAsStream("pirchdb.properties")) {
            if (inputStream != null) {
                PROPS.load(inputStream);
            }
        } catch (Exception e) {
            System.err.println("[PZLIFE][config] failed to load pirchdb.properties: " + e.getMessage());
        }
    }

    private PzRuntimeConfig() {
    }

    private static String get(String key, String def) {
        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? def : trimmed;
    }

    private static int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private static long getLong(String key, long def) {
        try {
            return Long.parseLong(get(key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(get(key, String.valueOf(def)));
    }

    public static boolean isIdentityDetectorEnabled() {
        return getBool("identity.detector.enabled", true);
    }

    public static long getIdentityDetectorPollMs() {
        return getLong("identity.detector.poll_ms", 3000L);
    }

    public static int getIdentityDetectorMaxAttempts() {
        return getInt("identity.detector.max_attempts", 0);
    }

    public static int getIdentityDetectorLogEveryAttempts() {
        return getInt("identity.detector.log_every_attempts", 10);
    }

    public static boolean isVerboseIdentityLoggingEnabled() {
        return getBool("identity.logging.verbose", false);
    }

    public static boolean isIdentitySessionMonitoringEnabled() {
        return getBool("identity.session.monitoring.enabled", true);
    }

    public static int getIdentitySessionEmptyChecksToRearm() {
        return getInt("identity.session.empty_checks_to_rearm", 3);
    }

    public static boolean isAuthSelfTestEnabled() {
        return getBool("auth.selftest.enabled", true);
    }

    public static String getAuthSelfTestNodeKey() {
        return get("auth.selftest.node_key", "debug:test-node");
    }

    public static String getAuthSelfTestNodeType() {
        return get("auth.selftest.node_type", "node");
    }

    public static String getAuthSelfTestPermissionKey() {
        return get("auth.selftest.permission_key", "ownership.manage");
    }

    public static String getAuthSelfTestScopeType() {
        return get("auth.selftest.scope_type", "node");
    }

    public static String getAuthSelfTestScopeKey() {
        return get("auth.selftest.scope_key", getAuthSelfTestNodeKey());
    }

    public static boolean isBootstrapAdminEnabled() {
        return getBool("auth.bootstrap_admin.enabled", true);
    }

    public static String getBootstrapAdminExternalId() {
        return get("auth.bootstrap_admin.external_id", "bootstrap:admin");
    }

    public static String getBootstrapAdminRoleKey() {
        return get("auth.bootstrap_admin.role_key", "admin");
    }

    public static String getBootstrapAdminDisplayName() {
        return get("auth.bootstrap_admin.display_name", "Bootstrap Admin");
    }

    public static boolean isAuthSelfTestTeardownEnabled() {
        return getBool("auth.selftest.teardown", false);
    }
}
