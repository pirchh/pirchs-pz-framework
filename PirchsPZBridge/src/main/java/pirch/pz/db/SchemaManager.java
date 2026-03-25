package pirch.pz.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import pirch.pzloader.util.LoaderLog;

public final class SchemaManager {
    private static final List<String> DEFAULT_ORDER = List.of(
        "001_schema.sql",
        "002_accounts.sql",
        "002_wallet.sql",
        "002_nodes.sql",
        "002_account_permissions.sql",
        "003_transactions.sql",
        "003_account_nodes.sql",
        "003_account_node.sql",
        "004_account_identity_columns.sql",
        "004_roles.sql",
        "005_role_permission.sql",
        "006_account_role.sql"
    );

    private static final Map<String, List<String>> FILES_BY_FOLDER = Map.of(
        "accounts", List.of(
            "001_schema.sql",
            "002_accounts.sql",
            "004_account_identity_columns.sql"
        ),
        "economy", List.of(
            "001_schema.sql",
            "002_wallet.sql",
            "003_transactions.sql"
        ),
        "ownership", List.of(
            "001_schema.sql",
            "002_nodes.sql",
            "003_account_node.sql"
        ),
        "permissions", List.of(
            "001_schema.sql",
            "002_account_permissions.sql",
            "003_account_nodes.sql"
        ),
        "roles", List.of(
            "001_schema.sql",
            "004_roles.sql",
            "005_role_permission.sql",
            "006_account_role.sql"
        )
    );

    private SchemaManager() {
    }

    public static void initialize() {
        if (!DatabaseConfig.isEnabled() || !DatabaseConfig.isAutoInitEnabled()) {
            LoaderLog.info("[PZLIFE][DB] schema auto-init skipped. enabled="
                + DatabaseConfig.isEnabled() + ", autoInit=" + DatabaseConfig.isAutoInitEnabled());
            return;
        }

        int executedCount = 0;
        for (String location : DatabaseConfig.getSchemaLocations()) {
            executedCount += runFolder(location);
        }

        LoaderLog.info("[PZLIFE][DB] schema initialization complete. executedResources=" + executedCount);
    }

    private static int runFolder(String folder) {
        String normalizedFolder = normalizeFolder(folder);
        List<String> orderedFiles = FILES_BY_FOLDER.getOrDefault(lastSegment(normalizedFolder), DEFAULT_ORDER);
        int executedCount = 0;

        for (String fileName : orderedFiles) {
            if (executeSqlIfPresent(normalizedFolder + "/" + fileName)) {
                executedCount++;
            }
        }

        LoaderLog.info("[PZLIFE][DB] processed schema folder " + normalizedFolder
            + ". attempted=" + orderedFiles.size() + ", executed=" + executedCount);

        return executedCount;
    }

    private static boolean executeSqlIfPresent(String resourcePath) {
        try (InputStream inputStream = SchemaManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return false;
            }

            StringBuilder sql = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sql.append(line).append('\n');
                }
            }

            String sqlText = sql.toString().trim();
            if (sqlText.isEmpty()) {
                LoaderLog.info("[PZLIFE][DB] skipped empty SQL resource: " + resourcePath);
                return false;
            }

            try (Connection connection = DatabaseManager.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(sqlText);
            }

            LoaderLog.info("[PZLIFE][DB] executed SQL resource: " + resourcePath);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed executing SQL resource: " + resourcePath, e);
        }
    }

    private static String normalizeFolder(String folder) {
        if (folder == null) {
            throw new IllegalArgumentException("Schema folder must not be null");
        }
        String trimmed = folder.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Schema folder must not be blank");
        }
        return trimmed.replace('\\', '/').replaceAll("/+$", "");
    }

    private static String lastSegment(String folder) {
        int index = folder.lastIndexOf('/');
        return index >= 0 ? folder.substring(index + 1) : folder;
    }
}
