package pirch.pz.service;

import pirch.pz.repo.PostgresAccountRepository;

public final class AccountService {
    private AccountService() {
    }

    public static int resolveOrCreateAccount(String externalId, String accountName) {
        validateExternalId(externalId);
        return PostgresAccountRepository.resolveOrCreateAccount(externalId, accountName);
    }

    public static int resolveOrCreateAccount(PlayerIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("identity cannot be null");
        }
        return PostgresAccountRepository.resolveOrCreateAccount(identity);
    }

    private static void validateExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId cannot be null or blank");
        }
    }
}
