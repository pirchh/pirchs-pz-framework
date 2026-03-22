package pirch.pz.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

public final class SchemaManager {
    private static final String[] KNOWN_FILES = {
        "001_schema.sql",
        "002_accounts.sql",
        "002_wallet.sql",
        "002_nodes.sql",
        "003_transactions.sql",
        "003_account_nodes.sql",
        "004_account_identity_columns.sql"
    };

    private SchemaManager() {
    }

    public static void initialize() {
        if (!DatabaseConfig.isEnabled() || !DatabaseConfig.isAutoInitEnabled()) {
            return;
        }

        for (String location : DatabaseConfig.getSchemaLocations()) {
            runFolder(location.trim());
        }
    }

    private static void runFolder(String folder) {
        for (String fileName : KNOWN_FILES) {
            executeSql(folder + "/" + fileName);
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
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            )) {
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
