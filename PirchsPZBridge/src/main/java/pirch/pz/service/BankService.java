package pirch.pz.service;

import pirch.pz.repo.PostgresAccountRepository;

public final class BankService {
    private BankService() {
    }

    public static int getBalance(String playerId) {
        validatePlayerId(playerId);
        return PostgresAccountRepository.getBalance(playerId);
    }

    public static int deposit(String playerId, int amount) {
        validatePlayerId(playerId);
        return PostgresAccountRepository.deposit(playerId, amount);
    }

    public static int withdraw(String playerId, int amount) {
        validatePlayerId(playerId);
        return PostgresAccountRepository.withdraw(playerId, amount);
    }

    private static void validatePlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId cannot be null or blank");
        }
    }
}