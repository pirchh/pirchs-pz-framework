package pirch.pz.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pirch.pz.db.DatabaseManager;
import pirch.pz.repo.PostgresAccountRepository;
import pirch.pz.repo.PostgresOwnershipRepository;
import pirch.pzloader.util.LoaderLog;

public final class OwnershipService {
    private OwnershipService() {
    }

    public static Map<String, Object> claimNode(PlayerIdentity identity, String nodeKey, String nodeType) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            int accountId = PostgresAccountRepository.resolveOrCreateAccount(identity);
            Map<String, Object> result =
                castMap(PostgresOwnershipRepository.claimNode(connection, accountId, nodeKey, nodeType));

            connection.commit();

            LoaderLog.info(
                "[PZLIFE][AUTH][ownership] claimNode result. accountId=" + accountId
                    + ", nodeKey=" + nodeKey
                    + ", nodeType=" + nodeType
                    + ", claimed=" + result.get("claimed")
                    + ", reason=" + result.get("reason")
            );
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(buildSqlFailureMessage("claim node", nodeKey, nodeType, e), e);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to claim node. nodeKey=" + nodeKey + ", nodeType=" + nodeType + ", cause=" + e.getMessage(),
                e
            );
        }
    }

    public static boolean releaseNode(PlayerIdentity identity, String nodeKey) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int accountId = PostgresAccountRepository.resolveOrCreateAccount(identity);

            boolean released = PostgresOwnershipRepository.releaseNode(connection, accountId, nodeKey);

            LoaderLog.info(
                "[PZLIFE][AUTH][ownership] releaseNode result. accountId=" + accountId
                    + ", nodeKey=" + nodeKey
                    + ", released=" + released
            );

            return released;
        } catch (SQLException e) {
            throw new RuntimeException(buildSqlFailureMessage("release node", nodeKey, null, e), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to release node. nodeKey=" + nodeKey + ", cause=" + e.getMessage(), e);
        }
    }

    public static Map<String, Object> getNodeOwner(String nodeKey) {
        try (Connection connection = DatabaseManager.getConnection()) {
            Map<String, Object> result = castMap(PostgresOwnershipRepository.getNodeOwner(connection, nodeKey));
            LoaderLog.info("[PZLIFE][AUTH][ownership] getNodeOwner result. nodeKey=" + nodeKey + ", result=" + result);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(buildSqlFailureMessage("get node owner", nodeKey, null, e), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node owner. nodeKey=" + nodeKey + ", cause=" + e.getMessage(), e);
        }
    }

    public static List<Map<String, Object>> listOwnedNodes(PlayerIdentity identity) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int accountId = PostgresAccountRepository.resolveOrCreateAccount(identity);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows =
                (List<Map<String, Object>>) (List<?>) PostgresOwnershipRepository.listOwnedNodes(connection, accountId);

            LoaderLog.info(
                "[PZLIFE][AUTH][ownership] listOwnedNodes result. accountId=" + accountId
                    + ", count=" + rows.size()
            );

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to list owned nodes for identity=" + identity.getAccountExternalId()
                    + ", sqlState=" + e.getSQLState()
                    + ", cause=" + e.getMessage(),
                e
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to list owned nodes for identity=" + identity.getAccountExternalId(), e);
        }
    }

    public static boolean isOwner(PlayerIdentity identity, String nodeKey) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int accountId = PostgresAccountRepository.resolveOrCreateAccount(identity);
            boolean owner = PostgresOwnershipRepository.isOwnerByNodeKey(connection, accountId, nodeKey);

            LoaderLog.info(
                "[PZLIFE][AUTH][ownership] isOwner result. accountId=" + accountId
                    + ", nodeKey=" + nodeKey
                    + ", owner=" + owner
            );

            return owner;
        } catch (SQLException e) {
            throw new RuntimeException(buildSqlFailureMessage("check node ownership", nodeKey, null, e), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check node ownership. nodeKey=" + nodeKey + ", cause=" + e.getMessage(), e);
        }
    }

    private static String buildSqlFailureMessage(String action, String nodeKey, String nodeType, SQLException e) {
        return "Failed to " + action
            + ". nodeKey=" + nodeKey
            + ", nodeType=" + nodeType
            + ", sqlState=" + e.getSQLState()
            + ", cause=" + e.getMessage();
    }

    private static Map<String, Object> castMap(Object value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        throw new IllegalStateException("Expected map result but got " + value.getClass().getName());
    }
}
