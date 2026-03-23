package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.postgresql.util.PSQLState;
import pirch.pz.db.DatabaseManager;
import pirch.pz.service.PlayerIdentity;

public final class PostgresAccountRepository {
    private PostgresAccountRepository() {
    }

    public static int resolveOrCreateAccount(String externalId, String accountName) {
        return resolveOrCreateAccount(PlayerIdentity.legacy(externalId));
    }

    public static int resolveOrCreateAccount(PlayerIdentity identity) {
        String accountExternalId = identity.getAccountExternalId();
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = resolveOrCreateAccountTransactional(connection, identity);
                connection.commit();
                return accountId;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to resolve or create account for accountExternalId=" + accountExternalId,
                e
            );
        }
    }

    public static int getBalance(PlayerIdentity identity) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int accountId = resolveOrCreateAccountTransactional(connection, identity);
            return getBalanceTransactional(connection, accountId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get balance for identity=" + identity.getAccountExternalId(), e);
        }
    }

    public static int deposit(PlayerIdentity identity, int amount) {
        validateAmount(amount);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = resolveOrCreateAccountTransactional(connection, identity);
                int currentBalance = getBalanceTransactional(connection, accountId);
                int newBalance = currentBalance + amount;
                updateBalance(connection, accountId, newBalance);
                insertTransaction(connection, accountId, "deposit", amount, newBalance);
                connection.commit();
                return newBalance;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deposit for identity=" + identity.getAccountExternalId(), e);
        }
    }

    public static int withdraw(PlayerIdentity identity, int amount) {
        validateAmount(amount);
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = resolveOrCreateAccountTransactional(connection, identity);
                int currentBalance = getBalanceTransactional(connection, accountId);
                if (currentBalance < amount) {
                    throw new IllegalStateException("Insufficient funds");
                }
                int newBalance = currentBalance - amount;
                updateBalance(connection, accountId, newBalance);
                insertTransaction(connection, accountId, "withdraw", amount, newBalance);
                connection.commit();
                return newBalance;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to withdraw for identity=" + identity.getAccountExternalId(), e);
        }
    }

    static int resolveOrCreateAccountTransactional(Connection connection, PlayerIdentity identity) throws SQLException {
        int accountId = findExistingAccountId(connection, identity);
        boolean created = false;

        if (accountId == -1) {
            accountId = createAccountOrResolveExisting(connection, identity);
            created = true;
        }

        touchAccount(connection, accountId, identity);

        if (created) {
            ensureDefaultManagePermission(connection, accountId);
        }

        ensureWallet(connection, accountId);
        return accountId;
    }

    private static int findExistingAccountId(Connection connection, PlayerIdentity identity) throws SQLException {
        int accountId = findAccountIdByCanonicalExternalId(connection, identity.getAccountExternalId());
        if (accountId != -1) {
            return accountId;
        }

        accountId = findAccountIdByLegacyIdentity(connection, identity);
        if (accountId != -1) {
            return accountId;
        }

        return -1;
    }

    private static int findAccountIdByCanonicalExternalId(Connection connection, String externalId) throws SQLException {
        if (normalize(externalId) == null) {
            return -1;
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id " +
                "FROM accounts.account " +
                "WHERE external_id = ? OR canonical_external_id = ?")) {
            statement.setString(1, externalId);
            statement.setString(2, externalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("account_id");
                }
            }
        }
        return -1;
    }

    private static int findAccountIdByLegacyIdentity(Connection connection, PlayerIdentity identity) throws SQLException {
        String playerSource = normalize(identity.getPlayerSource());
        String sourcePlayerId = normalize(identity.getSourcePlayerId());
        String steamId = normalize(identity.getSteamId());

        if (playerSource != null && sourcePlayerId != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_id " +
                    "FROM accounts.account " +
                    "WHERE player_source = ? AND source_player_id = ?")) {
                statement.setString(1, playerSource);
                statement.setString(2, sourcePlayerId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("account_id");
                    }
                }
            }
        }

        if (steamId != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_id " +
                    "FROM accounts.account " +
                    "WHERE steam_id = ?")) {
                statement.setString(1, steamId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("account_id");
                    }
                }
            }
        }

        return -1;
    }

    private static int createAccountOrResolveExisting(Connection connection, PlayerIdentity identity) throws SQLException {
        try {
            return createAccount(connection, identity);
        } catch (SQLException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }

            int existingAccountId = findExistingAccountId(connection, identity);
            if (existingAccountId != -1) {
                return existingAccountId;
            }

            throw e;
        }
    }

    private static int createAccount(Connection connection, PlayerIdentity identity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO accounts.account "
                + "(external_id, canonical_external_id, player_source, source_player_id, steam_id, account_name, "
                + " username_last_seen, display_name_last_seen, online_id_last_seen, "
                + " character_forename_last_seen, character_surname_last_seen, character_full_name_last_seen, "
                + " character_external_id_last_seen, identity_last_seen_at, last_seen_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) RETURNING account_id")) {
            statement.setString(1, identity.getAccountExternalId());
            statement.setString(2, identity.getAccountExternalId());
            statement.setString(3, normalize(identity.getPlayerSource()));
            statement.setString(4, normalize(identity.getSourcePlayerId()));
            statement.setString(5, normalize(identity.getSteamId()));
            statement.setString(6, normalize(identity.getPreferredAccountName()));
            statement.setString(7, normalize(identity.getUsername()));
            statement.setString(8, normalize(identity.getDisplayName()));
            statement.setString(9, normalize(identity.getOnlineId()));
            statement.setString(10, normalize(identity.getCharacterForename()));
            statement.setString(11, normalize(identity.getCharacterSurname()));
            statement.setString(12, normalize(identity.getCharacterFullName()));
            statement.setString(13, normalize(identity.getCharacterExternalId()));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Failed to create account for externalId=" + identity.getAccountExternalId());
                }
                return resultSet.getInt("account_id");
            }
        }
    }

    private static void touchAccount(Connection connection, int accountId, PlayerIdentity identity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE accounts.account SET "
                + "external_id = COALESCE(?, external_id), "
                + "canonical_external_id = COALESCE(?, canonical_external_id), "
                + "player_source = COALESCE(?, player_source), "
                + "source_player_id = COALESCE(?, source_player_id), "
                + "steam_id = COALESCE(?, steam_id), "
                + "account_name = COALESCE(?, account_name), "
                + "username_last_seen = COALESCE(?, username_last_seen), "
                + "display_name_last_seen = COALESCE(?, display_name_last_seen), "
                + "online_id_last_seen = COALESCE(?, online_id_last_seen), "
                + "character_forename_last_seen = COALESCE(?, character_forename_last_seen), "
                + "character_surname_last_seen = COALESCE(?, character_surname_last_seen), "
                + "character_full_name_last_seen = COALESCE(?, character_full_name_last_seen), "
                + "character_external_id_last_seen = COALESCE(?, character_external_id_last_seen), "
                + "identity_last_seen_at = NOW(), "
                + "last_seen_at = NOW() "
                + "WHERE account_id = ?")) {
            statement.setString(1, normalize(identity.getAccountExternalId()));
            statement.setString(2, normalize(identity.getAccountExternalId()));
            statement.setString(3, normalize(identity.getPlayerSource()));
            statement.setString(4, normalize(identity.getSourcePlayerId()));
            statement.setString(5, normalize(identity.getSteamId()));
            statement.setString(6, normalize(identity.getPreferredAccountName()));
            statement.setString(7, normalize(identity.getUsername()));
            statement.setString(8, normalize(identity.getDisplayName()));
            statement.setString(9, normalize(identity.getOnlineId()));
            statement.setString(10, normalize(identity.getCharacterForename()));
            statement.setString(11, normalize(identity.getCharacterSurname()));
            statement.setString(12, normalize(identity.getCharacterFullName()));
            statement.setString(13, normalize(identity.getCharacterExternalId()));
            statement.setInt(14, accountId);
            statement.executeUpdate();
        }
    }

    private static void ensureWallet(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO economy.wallet (account_id, balance, updated_at) VALUES (?, 0, NOW()) "
                + "ON CONFLICT (account_id) DO NOTHING")) {
            statement.setInt(1, accountId);
            statement.executeUpdate();
        }
    }

    private static int getBalanceTransactional(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT balance FROM economy.wallet WHERE account_id = ?")) {
            statement.setInt(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("balance");
                }
            }
        }
        return 0;
    }

    private static void updateBalance(Connection connection, int accountId, int newBalance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE economy.wallet SET balance = ?, updated_at = NOW() WHERE account_id = ?")) {
            statement.setInt(1, newBalance);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    private static void insertTransaction(
        Connection connection,
        int accountId,
        String type,
        int amount,
        int balanceAfter
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO economy.transactions (account_id, type, amount, balance_after) VALUES (?, ?, ?, ?)")) {
            statement.setInt(1, accountId);
            statement.setString(2, type);
            statement.setInt(3, amount);
            statement.setInt(4, balanceAfter);
            statement.executeUpdate();
        }
    }

    private static void ensureDefaultManagePermission(Connection connection, int accountId) throws SQLException {
        // 1. Try to update existing row (NULL-safe)
        try (PreparedStatement update = connection.prepareStatement(
            "UPDATE permissions.account_permission "
                + "SET active = TRUE, updated_at = NOW() "
                + "WHERE account_id = ? "
                + "AND permission_key = 'permissions.manage' "
                + "AND scope_type IS NULL "
                + "AND scope_key IS NULL")) {

            update.setInt(1, accountId);

            if (update.executeUpdate() > 0) {
                return; // already existed, we're done
            }
        }

        // 2. Insert only if it truly doesn't exist
        try (PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO permissions.account_permission "
                + "(account_id, permission_key, scope_type, scope_key, granted_by_account_id, active, created_at, updated_at) "
                + "SELECT ?, 'permissions.manage', NULL, NULL, NULL, TRUE, NOW(), NOW() "
                + "WHERE NOT EXISTS ("
                + "  SELECT 1 FROM permissions.account_permission "
                + "  WHERE account_id = ? "
                + "  AND permission_key = 'permissions.manage' "
                + "  AND scope_type IS NULL "
                + "  AND scope_key IS NULL"
                + ")")) {

            insert.setInt(1, accountId);
            insert.setInt(2, accountId);
            insert.executeUpdate();
        }
    }

    private static boolean isUniqueViolation(SQLException e) {
        SQLException current = e;
        while (current != null) {
            String sqlState = current.getSQLState();
            if (PSQLState.UNIQUE_VIOLATION.getState().equals(sqlState) || "23505".equals(sqlState)) {
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void validateAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
    }
}