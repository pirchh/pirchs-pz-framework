package pirch.pz.repo;

import pirch.pz.db.DatabaseManager;

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
                    accountId = createAccount(connection, externalId, accountName);
                } else {
                    touchAccount(connection, accountId, accountName);
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

    public static int getBalance(String externalId) {
        int accountId = resolveOrCreateAccount(externalId, externalId);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT balance
                 FROM economy.wallet
                 WHERE account_id = ?
             """)) {

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

    public static int deposit(String externalId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int accountId = findAccountId(connection, externalId);
                if (accountId == -1) {
                    accountId = createAccount(connection, externalId, externalId);
                } else {
                    touchAccount(connection, accountId, externalId);
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

    public static int withdraw(String externalId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int accountId = findAccountId(connection, externalId);
                if (accountId == -1) {
                    accountId = createAccount(connection, externalId, externalId);
                } else {
                    touchAccount(connection, accountId, externalId);
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

    private static int findAccountId(Connection connection, String externalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT account_id
            FROM accounts.account
            WHERE external_id = ?
        """)) {
            statement.setString(1, externalId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("account_id");
                }
            }
        }

        return -1;
    }

    private static int createAccount(Connection connection, String externalId, String accountName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO accounts.account (external_id, account_name, last_seen_at)
            VALUES (?, ?, NOW())
            RETURNING account_id
        """)) {
            statement.setString(1, externalId);
            statement.setString(2, normalizeAccountName(accountName));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Failed to create account for externalId=" + externalId);
                }

                return resultSet.getInt("account_id");
            }
        }
    }

    private static void touchAccount(Connection connection, int accountId, String accountName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            UPDATE accounts.account
            SET account_name = ?, last_seen_at = NOW()
            WHERE account_id = ?
        """)) {
            statement.setString(1, normalizeAccountName(accountName));
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    private static String normalizeAccountName(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            return null;
        }
        return accountName.trim();
    }

    private static void ensureWallet(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO economy.wallet (account_id, balance, updated_at)
            VALUES (?, 0, NOW())
            ON CONFLICT (account_id) DO NOTHING
        """)) {
            statement.setInt(1, accountId);
            statement.executeUpdate();
        }
    }

    private static int getBalanceTransactional(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT balance
            FROM economy.wallet
            WHERE account_id = ?
        """)) {
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
        try (PreparedStatement statement = connection.prepareStatement("""
            UPDATE economy.wallet
            SET balance = ?, updated_at = NOW()
            WHERE account_id = ?
        """)) {
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
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO economy.transactions (account_id, type, amount, balance_after)
            VALUES (?, ?, ?, ?)
        """)) {
            statement.setInt(1, accountId);
            statement.setString(2, type);
            statement.setInt(3, amount);
            statement.setInt(4, balanceAfter);
            statement.executeUpdate();
        }
    }
}