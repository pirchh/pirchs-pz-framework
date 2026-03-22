package pirch.pz.debug;

import java.util.Map;
import pirch.pz.service.AccountService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PzPlayerIdentityAdapter;
import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityLifecycleBridge {
    private static volatile boolean ready = false;
    private static volatile String lastCanonicalExternalId = null;

    private IdentityLifecycleBridge() {
    }

    public static void markReady() {
        ready = true;
    }

    public static boolean isReady() {
        return ready;
    }

    public static void onLocalPlayerCreated(int playerNum, IsoPlayer player) {
        if (!ready) {
            LoaderLog.info("[PZLIFE][IDENTITY][local] kickoff fired before bridge was ready.");
            return;
        }

        if (player == null) {
            LoaderLog.info("[PZLIFE][IDENTITY][local] kickoff received null player.");
            return;
        }

        try {
            PlayerIdentity identity = new PzPlayerIdentityAdapter().fromIsoPlayer(player);
            String canonicalId = identity.getCanonicalExternalId();

            if (canonicalId != null && canonicalId.equals(lastCanonicalExternalId)) {
                LoaderLog.info("[PZLIFE][IDENTITY][local] Identity already discovered for canonicalExternalId=" + canonicalId);
                return;
            }

            lastCanonicalExternalId = canonicalId;

            LoaderLog.info("[PZLIFE][IDENTITY][local] Identity discovered for playerNum=" + playerNum);
            logIdentity("local", identity);

            int accountId = AccountService.resolveOrCreateAccount(identity);
            LoaderLog.info("[PZLIFE][IDENTITY][local] RESOLVED_ACCOUNT_ID=" + accountId);
            LoaderLog.info("[PZLIFE][IDENTITY][local] RECOMMENDED_DB_KEY=" + identity.getCanonicalExternalId());
            LoaderLog.info("[PZLIFE][IDENTITY][local] RECOMMENDED_ACCOUNT_NAME=" + identity.getPreferredAccountName());
            LoaderLog.info("[PZLIFE][IDENTITY][local] RECOMMENDED_CHARACTER_NAME=" + identity.getPreferredCharacterName());
        } catch (Exception e) {
            LoaderLog.error("[PZLIFE][IDENTITY][local] Failed to resolve local player identity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void onServerPlayerReady(IsoPlayer player, String module, String command) {
        if (!ready) {
            LoaderLog.info("[PZLIFE][IDENTITY][server] PlayerReady fired before bridge was ready.");
            return;
        }

        if (player == null) {
            LoaderLog.info("[PZLIFE][IDENTITY][server] PlayerReady received null player.");
            return;
        }

        try {
            PlayerIdentity identity = new PzPlayerIdentityAdapter().fromIsoPlayer(player);

            LoaderLog.info("[PZLIFE][IDENTITY][server] PlayerReady received. module=" + module + ", command=" + command);
            logIdentity("server", identity);

            int accountId = AccountService.resolveOrCreateAccount(identity);
            LoaderLog.info("[PZLIFE][IDENTITY][server] RESOLVED_ACCOUNT_ID=" + accountId);
            LoaderLog.info("[PZLIFE][IDENTITY][server] RECOMMENDED_DB_KEY=" + identity.getCanonicalExternalId());
        } catch (Exception e) {
            LoaderLog.error("[PZLIFE][IDENTITY][server] Failed to resolve server player identity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void logIdentity(String scope, PlayerIdentity identity) {
        Map<String, Object> data = identity.toMap();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            LoaderLog.info("[PZLIFE][IDENTITY][" + scope + "] " + entry.getKey() + "=" + entry.getValue());
        }
    }
}
