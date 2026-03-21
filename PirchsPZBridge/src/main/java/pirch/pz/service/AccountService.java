package pirch.pz.service;

import pirch.pz.repo.PostgresAccountRepository;

public final class AccountService {
    private AccountService() {
    }

    public static int resolveOrCreateAccount(String externalId, String accountName) {
        validateExternalId(externalId);
        return PostgresAccountRepository.resolveOrCreateAccount(externalId, accountName);
    }

    private static void validateExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId cannot be null or blank");
        }
    }
}