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

    public static int countCarriedMoney() {
        LocalPlayerIdentityResolver.requireLocalIdentity();
        return CashInventoryService.countLocalPlayerMoney();
    }

    public static BankTransactionResult depositCarriedMoney(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);

        int balanceBefore = getBalance(identity);
        int carriedBefore = CashInventoryService.countLocalPlayerMoney();
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        if (carriedBefore < amount) {
            throw new IllegalStateException("You tried to deposit more money than you are carrying");
        }

        int processed = CashInventoryService.removeLocalPlayerMoney(amount);
        int balanceAfter = PostgresAccountRepository.deposit(identity, processed);
        int carriedAfter = CashInventoryService.countLocalPlayerMoney();

        return new BankTransactionResult(
            "depositCarriedMoney",
            amount,
            processed,
            balanceBefore,
            balanceAfter,
            carriedBefore,
            carriedAfter,
            true,
            null
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
        int carriedBefore = CashInventoryService.countLocalPlayerMoney();
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
        int carriedAfter = CashInventoryService.countLocalPlayerMoney();

        return new BankTransactionResult(
            "withdrawCashToInventory",
            amount,
            granted,
            balanceBefore,
            balanceAfter,
            carriedBefore,
            carriedAfter,
            true,
            null
        );
    }
}
