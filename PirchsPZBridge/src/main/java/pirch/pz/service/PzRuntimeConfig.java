package pirch.pz.service;

import java.io.InputStream;
import java.util.Properties;

public final class PzRuntimeConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = PzRuntimeConfig.class.getClassLoader().getResourceAsStream("pirchdb.properties")) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed loading pirchdb.properties", e);
        }
    }

    private PzRuntimeConfig() {
    }

    public static boolean isIdentityDetectorEnabled() {
        return Boolean.parseBoolean(read("pirch.identity.detector.enabled", "pirch.identity.watcher.enabled", "true"));
    }

    public static long getIdentityDetectorPollMs() {
        return Long.parseLong(read("pirch.identity.detector.poll_ms", "pirch.identity.watcher.poll_ms", "3000").trim());
    }

    public static int getIdentityDetectorMaxAttempts() {
        return Integer.parseInt(read("pirch.identity.detector.max_attempts", "pirch.identity.watcher.max_attempts", "0").trim());
    }

    public static int getIdentityDetectorLogEveryAttempts() {
        return Integer.parseInt(read("pirch.identity.detector.log_every_attempts", "pirch.identity.watcher.log_every_attempts", "10").trim());
    }

    public static boolean isIdentitySessionMonitoringEnabled() {
        return Boolean.parseBoolean(read("pirch.identity.detector.monitor_after_resolve", null, "true"));
    }

    public static int getIdentitySessionEmptyChecksToRearm() {
        return Integer.parseInt(read("pirch.identity.detector.empty_checks_to_rearm", null, "3").trim());
    }

    public static boolean isVerboseIdentityLoggingEnabled() {
        return Boolean.parseBoolean(read("pirch.identity.logging.verbose", null, "true"));
    }

    private static String read(String primaryKey, String legacyKey, String fallback) {
        String value = PROPERTIES.getProperty(primaryKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (legacyKey != null) {
            value = PROPERTIES.getProperty(legacyKey);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }
}
