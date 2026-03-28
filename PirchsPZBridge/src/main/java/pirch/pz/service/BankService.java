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
        validateAmount(amount);
        return PostgresAccountRepository.deposit(identity, amount);
    }

    public static int withdraw(String playerId, int amount) {
        return withdraw(PlayerIdentity.legacy(playerId), amount);
    }

    public static int withdraw(PlayerIdentity identity, int amount) {
        PlayerIdentityService.validate(identity);
        validateAmount(amount);
        return PostgresAccountRepository.withdraw(identity, amount);
    }

    public static BankSnapshot snapshot(PlayerIdentity identity, boolean localIdentity, String source) {
        PlayerIdentityService.validate(identity);
        return new BankSnapshot(identity, getBalance(identity), localIdentity, source);
    }

    public static BankSnapshot localSnapshot() {
        PlayerIdentity identity = LocalPlayerIdentityResolver.requireLocalIdentity();
        return snapshot(identity, true, "local-resolver");
    }

    private static void validateAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }
}
