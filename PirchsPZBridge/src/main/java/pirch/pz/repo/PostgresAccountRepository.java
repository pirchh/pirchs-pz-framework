package pirch.pz.repo;

import pirch.pz.db.DatabaseManager;
import pirch.pz.service.PlayerIdentity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class PostgresAccountRepository {
    private PostgresAccountRepository() {
    }

    public static int resolveOrCreateAccount(String externalId, String accountName) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = findAccountId(connection, externalId);
                if (accountId == -1) {
                    accountId = createAccount(connection, externalId, externalId, null, null, null, accountName, null, null, null);
                } else {
                    touchAccount(connection, accountId, externalId, null, null, null, accountName, null, null, null);
                }
                ensureWallet(connection, accountId);
                connection.commit();
                return accountId;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve or create account for externalId=" + externalId, e);
        }
    }

    public static int resolveOrCreateAccount(PlayerIdentity identity) {
        String canonicalExternalId = identity.getCanonicalExternalId();
        String accountName = identity.getPreferredAccountName();

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = findAccountId(connection, canonicalExternalId);
                if (accountId == -1) {
                    accountId = createAccount(
                        connection,
                        canonicalExternalId,
                        canonicalExternalId,
                        identity.getPlayerSource(),
                        identity.getSourcePlayerId(),
                        identity.getSteamId(),
                        accountName,
                        identity.getUsername(),
                        identity.getDisplayName(),
                        identity.getOnlineId()
                    );
                    touchCharacterFields(connection, accountId, identity);
                } else {
                    touchAccount(
                        connection,
                        accountId,
                        canonicalExternalId,
                        identity.getPlayerSource(),
                        identity.getSourcePlayerId(),
                        identity.getSteamId(),
                        accountName,
                        identity.getUsername(),
                        identity.getDisplayName(),
                        identity.getOnlineId()
                    );
                    touchCharacterFields(connection, accountId, identity);
                }

                ensureWallet(connection, accountId);
                connection.commit();
                return accountId;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve or create account for canonicalExternalId=" + canonicalExternalId, e);
        }
    }

    public static int getBalance(String externalId) {
        int accountId = resolveOrCreateAccount(externalId, externalId);
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT balance FROM economy.wallet WHERE account_id = ?"
             )) {
            statement.setInt(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("balance");
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get balance for externalId=" + externalId, e);
        }
    }

    public static int getBalance(PlayerIdentity identity) {
        int accountId = resolveOrCreateAccount(identity);
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT balance FROM economy.wallet WHERE account_id = ?"
             )) {
            statement.setInt(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("balance");
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get balance for identity=" + identity.getCanonicalExternalId(), e);
        }
    }

    public static int deposit(String externalId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = findAccountId(connection, externalId);
                if (accountId == -1) {
                    accountId = createAccount(connection, externalId, externalId, null, null, null, externalId, null, null, null);
                } else {
                    touchAccount(connection, accountId, externalId, null, null, null, externalId, null, null, null);
                }

                ensureWallet(connection, accountId);
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
            throw new RuntimeException("Failed to deposit for externalId=" + externalId, e);
        }
    }

    public static int deposit(PlayerIdentity identity, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = resolveOrCreateAccount(identity);
                ensureWallet(connection, accountId);
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
            throw new RuntimeException("Failed to deposit for identity=" + identity.getCanonicalExternalId(), e);
        }
    }

    public static int withdraw(String externalId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = findAccountId(connection, externalId);
                if (accountId == -1) {
                    accountId = createAccount(connection, externalId, externalId, null, null, null, externalId, null, null, null);
                } else {
                    touchAccount(connection, accountId, externalId, null, null, null, externalId, null, null, null);
                }

                ensureWallet(connection, accountId);
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
            throw new RuntimeException("Failed to withdraw for externalId=" + externalId, e);
        }
    }

    public static int withdraw(PlayerIdentity identity, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = resolveOrCreateAccount(identity);
                ensureWallet(connection, accountId);
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
            throw new RuntimeException("Failed to withdraw for identity=" + identity.getCanonicalExternalId(), e);
        }
    }

    private static int findAccountId(Connection connection, String externalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id FROM accounts.account WHERE external_id = ? OR canonical_external_id = ?"
        )) {
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

    private static int createAccount(
        Connection connection,
        String externalId,
        String canonicalExternalId,
        String playerSource,
        String sourcePlayerId,
        String steamId,
        String accountName,
        String username,
        String displayName,
        String onlineId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO accounts.account " +
                "(external_id, canonical_external_id, player_source, source_player_id, steam_id, account_name, " +
                " username_last_seen, display_name_last_seen, online_id_last_seen, identity_last_seen_at, last_seen_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) RETURNING account_id"
        )) {
            statement.setString(1, externalId);
            statement.setString(2, canonicalExternalId);
            statement.setString(3, normalize(playerSource));
            statement.setString(4, normalize(sourcePlayerId));
            statement.setString(5, normalize(steamId));
            statement.setString(6, normalizeAccountName(accountName));
            statement.setString(7, normalize(username));
            statement.setString(8, normalize(displayName));
            statement.setString(9, normalize(onlineId));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Failed to create account for externalId=" + externalId);
                }
                return resultSet.getInt("account_id");
            }
        }
    }

    private static void touchAccount(
        Connection connection,
        int accountId,
        String canonicalExternalId,
        String playerSource,
        String sourcePlayerId,
        String steamId,
        String accountName,
        String username,
        String displayName,
        String onlineId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE accounts.account SET " +
                "external_id = ?, " +
                "canonical_external_id = ?, " +
                "player_source = COALESCE(?, player_source), " +
                "source_player_id = COALESCE(?, source_player_id), " +
                "steam_id = COALESCE(?, steam_id), " +
                "account_name = COALESCE(?, account_name), " +
                "username_last_seen = COALESCE(?, username_last_seen), " +
                "display_name_last_seen = COALESCE(?, display_name_last_seen), " +
                "online_id_last_seen = COALESCE(?, online_id_last_seen), " +
                "identity_last_seen_at = NOW(), " +
                "last_seen_at = NOW() " +
                "WHERE account_id = ?"
        )) {
            statement.setString(1, canonicalExternalId);
            statement.setString(2, canonicalExternalId);
            statement.setString(3, normalize(playerSource));
            statement.setString(4, normalize(sourcePlayerId));
            statement.setString(5, normalize(steamId));
            statement.setString(6, normalizeAccountName(accountName));
            statement.setString(7, normalize(username));
            statement.setString(8, normalize(displayName));
            statement.setString(9, normalize(onlineId));
            statement.setInt(10, accountId);
            statement.executeUpdate();
        }
    }

    private static void touchCharacterFields(Connection connection, int accountId, PlayerIdentity identity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE accounts.account SET " +
                "character_forename_last_seen = COALESCE(?, character_forename_last_seen), " +
                "character_surname_last_seen = COALESCE(?, character_surname_last_seen), " +
                "character_full_name_last_seen = COALESCE(?, character_full_name_last_seen), " +
                "identity_last_seen_at = NOW() " +
                "WHERE account_id = ?"
        )) {
            statement.setString(1, normalize(identity.getCharacterForename()));
            statement.setString(2, normalize(identity.getCharacterSurname()));
            statement.setString(3, normalize(identity.getCharacterFullName()));
            statement.setInt(4, accountId);
            statement.executeUpdate();
        }
    }

    private static String normalizeAccountName(String accountName) {
        return normalize(accountName);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void ensureWallet(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO economy.wallet (account_id, balance, updated_at) VALUES (?, 0, NOW()) " +
                "ON CONFLICT (account_id) DO NOTHING"
        )) {
            statement.setInt(1, accountId);
            statement.executeUpdate();
        }
    }

    private static int getBalanceTransactional(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT balance FROM economy.wallet WHERE account_id = ?"
        )) {
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
            "UPDATE economy.wallet SET balance = ?, updated_at = NOW() WHERE account_id = ?"
        )) {
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
            "INSERT INTO economy.transactions (account_id, type, amount, balance_after) VALUES (?, ?, ?, ?)"
        )) {
            statement.setInt(1, accountId);
            statement.setString(2, type);
            statement.setInt(3, amount);
            statement.setInt(4, balanceAfter);
            statement.executeUpdate();
        }
    }
}