package pirch.pz.service;

import java.sql.Connection;
import java.sql.SQLException;
import pirch.pz.repo.PostgresAccountRepository;

public final class BootstrapAdminService {
    private BootstrapAdminService() {}

    public static int ensureBootstrapAdmin(Connection connection) throws SQLException {
        RoleService.ensureCoreRoles(connection);
        PlayerIdentity identity = PlayerIdentity.legacy(PzRuntimeConfig.getBootstrapAdminExternalId());
        int accountId = PostgresAccountRepository.resolveOrCreateAccount(identity);
        RoleService.assignRole(connection, accountId, PzRuntimeConfig.getBootstrapAdminRoleKey(), null);
        return accountId;
    }
}
