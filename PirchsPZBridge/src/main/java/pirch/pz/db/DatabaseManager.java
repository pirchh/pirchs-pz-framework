package pirch.pz.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        if (!DatabaseConfig.isEnabled()) {
            throw new IllegalStateException("Database is disabled in pirchdb.properties");
        }
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PostgreSQL JDBC driver not found on runtime classpath", e);
        }
        return DriverManager.getConnection(
            DatabaseConfig.getJdbcUrl(),
            DatabaseConfig.getUser(),
            DatabaseConfig.getPassword()
        );
    }
}
