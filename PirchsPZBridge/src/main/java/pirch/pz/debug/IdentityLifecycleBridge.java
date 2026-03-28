package pirch.pz.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.PlayerIdentity;
import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityLifecycleBridge {
    private static final String SOURCE_JAVA_DISCOVERY = "java-discovery";
    private static final String SOURCE_SERVER_READY = "lua-server-ready";
    private static final Object RESOLUTION_LOCK = new Object();

    private IdentityLifecycleBridge() {
    }

    public static void markReady() {
        IdentityLifecycleService.markReady();
    }

    public static boolean isReady() {
        return IdentityLifecycleService.isReady();
    }

    public static void onLocalPlayerCreated(int playerNum, IsoPlayer player) {
        resolve(playerNum, player, SOURCE_JAVA_DISCOVERY, true);
    }

    public static void onServerPlayerReady(IsoPlayer player, String module, String command) {
        LoaderLog.info(
            "[PZLIFE][IDENTITY][server] PlayerReady received. module=" + module
                + ", command=" + command
                + ", priority=non-authoritative"
        );
        resolve(0, player, SOURCE_SERVER_READY, false);
    }

    public static Map<String, Object> diagnostics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", IdentityLifecycleService.isReady());
        result.put("hasResolvedLocalAccount", IdentityLifecycleService.hasResolvedLocalAccount());
        result.put("lastResolutionSource", IdentityLifecycleService.getLastResolutionSource());
        result.put("lastResolutionAuthoritative", IdentityLifecycleService.isLastResolutionAuthoritative());
        result.put("lastResolvedAccountId", IdentityLifecycleService.getLastResolvedAccountId());
        result.put("lastResolvedAccountExternalId", IdentityLifecycleService.getLastResolvedAccountExternalId());
        result.put("lastResolvedPlayerNum", IdentityLifecycleService.getLastResolvedPlayerNum());
        result.put("lastResolutionEpochMs", IdentityLifecycleService.getLastResolutionEpochMs());
        return result;
    }

    private static void resolve(int playerNum, IsoPlayer player, String source, boolean authoritative) {
        synchronized (RESOLUTION_LOCK) {
            try {
                if (!authoritative
                    && IdentityLifecycleService.hasResolvedLocalAccount()
                    && IdentityLifecycleService.isLastResolutionAuthoritative()) {
                    if (IdentityLifecycleService.getLastIdentity() != null) {
                        LoaderLog.info(
                            "[PZLIFE][IDENTITY] ignoring lower-priority lifecycle signal because Java-authoritative identity "
                                + "is already active. source=" + source
                                + ", activeSource=" + IdentityLifecycleService.getLastResolutionSource()
                        );
                        return;
                    }
                }

                PlayerIdentity identity = IdentityLifecycleService.resolveAndPromoteLocalPlayer(
                    playerNum,
                    player,
                    source,
                    authoritative
                );
                if (identity == null) {
                    return;
                }

                LoaderLog.info(
                    "[PZLIFE][IDENTITY] lifecycle promotion succeeded from source=" + source
                        + ", authoritative=" + authoritative
                );
                Map<String, Object> data = identity.toMap();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    LoaderLog.info("[PZLIFE][IDENTITY][local] " + entry.getKey() + "=" + entry.getValue());
                }
            } catch (Exception e) {
                LoaderLog.error(
                    "[PZLIFE][IDENTITY][local] Failed to resolve local player identity. source=" + source
                        + ", cause=" + e.getMessage()
                );
                e.printStackTrace();
            }
        }
    }
}
