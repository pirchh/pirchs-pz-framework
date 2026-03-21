package pirch.pz.db;

import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = DatabaseConfig.class
            .getClassLoader()
            .getResourceAsStream("pirchdb.properties")) {

            if (inputStream == null) {
                throw new IllegalStateException("pirchdb.properties not found");
            }

            PROPERTIES.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load pirchdb.properties", e);
        }
    }

    private DatabaseConfig() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("pirchdb.enabled", "true"));
    }

    public static String getType() {
        return PROPERTIES.getProperty("pirchdb.type", "postgres").trim();
    }

    public static String getHost() {
        return PROPERTIES.getProperty("pirchdb.host", "127.0.0.1").trim();
    }

    public static int getPort() {
        return Integer.parseInt(PROPERTIES.getProperty("pirchdb.port", "5432").trim());
    }

    public static String getDatabaseName() {
        return PROPERTIES.getProperty("pirchdb.name", "pzlife").trim();
    }

    public static String getUser() {
        return PROPERTIES.getProperty("pirchdb.user", "postgres").trim();
    }

    public static String getPassword() {
        return PROPERTIES.getProperty("pirchdb.password", "postgres").trim();
    }

    public static boolean isSslEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("pirchdb.ssl", "false"));
    }

    public static boolean isAutoInitEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("pirchdb.schema.auto_init", "true"));
    }

    public static String[] getSchemaLocations() {
        String raw = PROPERTIES.getProperty(
            "pirchdb.schema.locations",
            "sql/accounts,sql/economy,sql/ownership,sql/permissions"
        );

        return raw.split(",");
    }

    public static String getJdbcUrl() {
        String overrideUrl = PROPERTIES.getProperty("pirchdb.url", "").trim();
        if (!overrideUrl.isEmpty()) {
            return overrideUrl;
        }

        if (!"postgres".equalsIgnoreCase(getType())) {
            throw new IllegalStateException("Unsupported database type: " + getType());
        }

        return "jdbc:postgresql://"
            + getHost()
            + ":"
            + getPort()
            + "/"
            + getDatabaseName()
            + "?ssl="
            + isSslEnabled();
    }
}