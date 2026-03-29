package pirch.pz.service;

import pirch.pz.repo.PostgresAccountRepository;

public final class BankService {
    private BankService() {
    }

    public static int getBalance(String playerId) {
        return getBalance(PlayerIdentity.legacy(playerId));
    }

    public static int getBalance(PlayerIdentity identity) {
        PlayerIdentityService.validate(identity);
        return PostgresAccountRepository.getBalance(identity);
    }

    public static int deposit(String playerId, int amount) {
        return deposit(PlayerIdentity.legacy(playerId), amount);
    }

    public static int deposit(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);
        return PostgresAccountRepository.deposit(identity, amount);
    }

    public static int withdraw(String playerId, int amount) {
        return withdraw(PlayerIdentity.legacy(playerId), amount);
    }

    public static int withdraw(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);
        return PostgresAccountRepository.withdraw(identity, amount);
    }

    public static BankSnapshot localSnapshot() {
        PlayerIdentity identity = LocalPlayerIdentityResolver.requireLocalIdentity();
        return new BankSnapshot(
            identity,
            getBalance(identity),
            true,
            LocalPlayerIdentityResolver.getResolutionSource()
        );
    }

    public static CashInventorySnapshot getCashInventorySnapshot() {
        LocalPlayerIdentityResolver.requireLocalIdentity();
        return CashInventoryService.snapshotLocalPlayerMoney();
    }

    public static int countCarriedMoney() {
        return getCashInventorySnapshot().totalMoney();
    }

    public static BankTransactionResult depositCarriedMoney(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);

        int balanceBefore = getBalance(identity);
        CashInventorySnapshot before = CashInventoryService.snapshotLocalPlayerMoney();
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        if (before.totalMoney() < amount) {
            throw new IllegalStateException("You tried to deposit more money than you are carrying");
        }

        int processed = CashInventoryService.removeLocalPlayerMoney(amount);
        int balanceAfter = PostgresAccountRepository.deposit(identity, processed);
        CashInventorySnapshot after = CashInventoryService.snapshotLocalPlayerMoney();

        return transactionResult(
            "depositCarriedMoney",
            amount,
            processed,
            balanceBefore,
            balanceAfter,
            before,
            after
        );
    }

    public static BankTransactionResult depositAllCarriedMoney(PlayerIdentity identity) {
        PlayerIdentityService.validate(identity);

        int carriedBefore = CashInventoryService.countLocalPlayerMoney();
        if (carriedBefore <= 0) {
            throw new IllegalStateException("You are not carrying any money");
        }

        return depositCarriedMoney(identity, carriedBefore);
    }

    public static BankTransactionResult withdrawCashToInventory(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);

        int balanceBefore = getBalance(identity);
        CashInventorySnapshot before = CashInventoryService.snapshotLocalPlayerMoney();
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be greater than 0");
        }
        if (balanceBefore < amount) {
            throw new IllegalStateException("Insufficient funds");
        }

        int granted = CashInventoryService.giveLocalPlayerMoney(amount);
        if (granted != amount) {
            throw new IllegalStateException("Unable to place withdrawn cash in inventory");
        }

        int balanceAfter = PostgresAccountRepository.withdraw(identity, granted);
        CashInventorySnapshot after = CashInventoryService.snapshotLocalPlayerMoney();

        return transactionResult(
            "withdrawCashToInventory",
            amount,
            granted,
            balanceBefore,
            balanceAfter,
            before,
            after
        );
    }

    public static BankTransactionResult moveCashToWallet(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);

        int balanceBefore = getBalance(identity);
        CashInventorySnapshot before = CashInventoryService.snapshotLocalPlayerMoney();
        if (amount <= 0) {
            throw new IllegalArgumentException("Wallet transfer amount must be greater than 0");
        }

        int moved = CashInventoryService.moveLooseLocalPlayerMoneyToWallet(amount);
        CashInventorySnapshot after = CashInventoryService.snapshotLocalPlayerMoney();

        return transactionResult(
            "moveCashToWallet",
            amount,
            moved,
            balanceBefore,
            balanceBefore,
            before,
            after
        );
    }

    public static BankTransactionResult moveCashFromWallet(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);

        int balanceBefore = getBalance(identity);
        CashInventorySnapshot before = CashInventoryService.snapshotLocalPlayerMoney();
        if (amount <= 0) {
            throw new IllegalArgumentException("Wallet transfer amount must be greater than 0");
        }

        int moved = CashInventoryService.moveWalletLocalPlayerMoneyToInventory(amount);
        CashInventorySnapshot after = CashInventoryService.snapshotLocalPlayerMoney();

        return transactionResult(
            "moveCashFromWallet",
            amount,
            moved,
            balanceBefore,
            balanceBefore,
            before,
            after
        );
    }

    public static BankTransactionResult moveAllLooseCashToWallet(PlayerIdentity identity) {
        PlayerIdentityService.validate(identity);

        int looseBefore = CashInventoryService.countLooseLocalPlayerMoney();
        if (looseBefore <= 0) {
            throw new IllegalStateException("You are not carrying any loose cash");
        }

        return moveCashToWallet(identity, looseBefore);
    }

    public static BankTransactionResult moveAllWalletCashToInventory(PlayerIdentity identity) {
        PlayerIdentityService.validate(identity);

        int walletBefore = CashInventoryService.countWalletLocalPlayerMoney();
        if (walletBefore <= 0) {
            throw new IllegalStateException("Your wallet is not carrying any cash");
        }

        return moveCashFromWallet(identity, walletBefore);
    }

    private static BankTransactionResult transactionResult(
        String action,
        int requestedAmount,
        int processedAmount,
        int balanceBefore,
        int balanceAfter,
        CashInventorySnapshot before,
        CashInventorySnapshot after
    ) {
        return new BankTransactionResult(
            action,
            requestedAmount,
            processedAmount,
            balanceBefore,
            balanceAfter,
            before.totalMoney(),
            after.totalMoney(),
            before.looseMoney(),
            after.looseMoney(),
            before.walletMoney(),
            after.walletMoney(),
            true,
            null
        );
    }
}
