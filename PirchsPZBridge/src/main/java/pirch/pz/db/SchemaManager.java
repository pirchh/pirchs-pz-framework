package pirch.pz.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SchemaManager {
    private static final Map<String, String[]> KNOWN_FILES = new LinkedHashMap<>();

    static {
        KNOWN_FILES.put("sql/accounts", new String[] {
            "001_schema.sql",
            "002_accounts.sql",
            "004_account_identity_columns.sql"
        });
        KNOWN_FILES.put("sql/economy", new String[] {
            "001_schema.sql",
            "002_wallet.sql",
            "003_transactions.sql"
        });
        KNOWN_FILES.put("sql/ownership", new String[] {
            "001_schema.sql",
            "002_nodes.sql",
            "003_account_nodes.sql"
        });
        KNOWN_FILES.put("sql/permissions", new String[] {
            "001_schema.sql",
            "002_account_permissions.sql"
        });
    }

    private SchemaManager() {
    }

    public static void initialize() {
        if (!DatabaseConfig.isEnabled() || !DatabaseConfig.isAutoInitEnabled()) {
            return;
        }

        for (String rawLocation : DatabaseConfig.getSchemaLocations()) {
            String location = rawLocation.trim();
            String[] fileNames = KNOWN_FILES.get(location);
            if (fileNames == null) {
                continue;
            }
            for (String fileName : fileNames) {
                executeSql(location + "/" + fileName);
            }
        }
    }

    private static void executeSql(String resourcePath) {
        try (InputStream inputStream = SchemaManager.class
            .getClassLoader()
            .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return;
            }

            StringBuilder sql = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sql.append(line).append('\n');
                }
            }

            String sqlText = sql.toString().trim();
            if (sqlText.isEmpty()) {
                return;
            }

            try (Connection connection = DatabaseManager.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(sqlText);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed executing SQL resource: " + resourcePath, e);
        }
    }
}
