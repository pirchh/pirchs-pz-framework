package pirch.pz.service;

import java.io.InputStream;
import java.util.Properties;

public final class PzRuntimeConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = PzRuntimeConfig.class.getClassLoader()
            .getResourceAsStream("pirchdb.properties")) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed loading pirchdb.properties", e);
        }
    }

    private PzRuntimeConfig() {
    }

    public static boolean isIdentityWatcherEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("pirch.identity.watcher.enabled", "true"));
    }

    public static long getIdentityWatcherPollMs() {
        return Long.parseLong(PROPERTIES.getProperty("pirch.identity.watcher.poll_ms", "3000").trim());
    }

    public static int getIdentityWatcherMaxAttempts() {
        return Integer.parseInt(PROPERTIES.getProperty("pirch.identity.watcher.max_attempts", "0").trim());
    }

    public static int getIdentityWatcherLogEveryAttempts() {
        return Integer.parseInt(PROPERTIES.getProperty("pirch.identity.watcher.log_every_attempts", "10").trim());
    }

    public static boolean isVerboseIdentityLoggingEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("pirch.identity.logging.verbose", "true"));
    }
}
