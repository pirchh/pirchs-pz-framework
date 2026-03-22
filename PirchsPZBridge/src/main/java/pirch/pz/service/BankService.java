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
}
