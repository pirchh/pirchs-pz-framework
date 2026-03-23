package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PostgresOwnershipRepository {
    private PostgresOwnershipRepository() {
    }

    public static void claim(Connection connection, int accountId, String nodeType, String nodeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO ownership.account_node " +
                "(account_id, node_type, node_key, active, created_at, updated_at) " +
                "VALUES (?, ?, ?, TRUE, NOW(), NOW()) " +
                "ON CONFLICT (node_key) DO UPDATE SET " +
                "account_id = EXCLUDED.account_id, " +
                "node_type = EXCLUDED.node_type, " +
                "active = TRUE, " +
                "updated_at = NOW()")) {
            statement.setInt(1, accountId);
            statement.setString(2, normalize(nodeType));
            statement.setString(3, normalize(nodeKey));
            statement.executeUpdate();
        }
    }

    public static Map<String, Object> claimNode(Connection connection, int accountId, String nodeKey, String nodeType)
        throws SQLException {
        String normalizedNodeType = normalize(nodeType);
        String normalizedNodeKey = normalize(nodeKey);

        if (normalizedNodeType == null) {
            throw new SQLException("nodeType cannot be null/blank");
        }
        if (normalizedNodeKey == null) {
            throw new SQLException("nodeKey cannot be null/blank");
        }

        claim(connection, accountId, normalizedNodeType, normalizedNodeKey);

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("accountId", accountId);
        result.put("nodeType", normalizedNodeType);
        result.put("nodeKey", normalizedNodeKey);
        result.put("claimed", true);
        result.put("active", true);
        return result;
    }

    public static boolean releaseNode(Connection connection, int accountId, String nodeKey) throws SQLException {
        String normalizedNodeKey = normalize(nodeKey);

        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE ownership.account_node " +
                "SET active = FALSE, updated_at = NOW() " +
                "WHERE account_id = ? AND node_key = ? AND active = TRUE")) {
            statement.setInt(1, accountId);
            statement.setString(2, normalizedNodeKey);
            return statement.executeUpdate() > 0;
        }
    }

    public static Map<String, Object> getNodeOwner(Connection connection, String nodeKey) throws SQLException {
        String normalizedNodeKey = normalize(nodeKey);

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id, node_type, node_key, active, created_at, updated_at " +
                "FROM ownership.account_node " +
                "WHERE node_key = ? AND active = TRUE " +
                "LIMIT 1")) {
            statement.setString(1, normalizedNodeKey);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Map<String, Object> result = new HashMap<>();
                result.put("accountId", rs.getInt("account_id"));
                result.put("nodeType", rs.getString("node_type"));
                result.put("nodeKey", rs.getString("node_key"));
                result.put("active", rs.getBoolean("active"));
                result.put("createdAt", rs.getTimestamp("created_at"));
                result.put("updatedAt", rs.getTimestamp("updated_at"));
                return result;
            }
        }
    }

    public static List<Map<String, Object>> listOwnedNodes(Connection connection, int accountId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id, node_type, node_key, active, created_at, updated_at " +
                "FROM ownership.account_node " +
                "WHERE account_id = ? AND active = TRUE " +
                "ORDER BY node_type ASC, node_key ASC")) {
            statement.setInt(1, accountId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("accountId", rs.getInt("account_id"));
                    row.put("nodeType", rs.getString("node_type"));
                    row.put("nodeKey", rs.getString("node_key"));
                    row.put("active", rs.getBoolean("active"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    row.put("updatedAt", rs.getTimestamp("updated_at"));
                    results.add(row);
                }
            }
        }

        return results;
    }

    public static boolean isOwner(Connection connection, int accountId, String nodeType, String nodeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 " +
                "FROM ownership.account_node " +
                "WHERE account_id = ? AND node_type = ? AND node_key = ? AND active = TRUE")) {
            statement.setInt(1, accountId);
            statement.setString(2, normalize(nodeType));
            statement.setString(3, normalize(nodeKey));

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}