package pirch.pz.debug;

import java.util.Map;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.PlayerIdentity;
import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityLifecycleBridge {
    private IdentityLifecycleBridge() {
    }

    public static void markReady() {
        IdentityLifecycleService.markReady();
    }

    public static boolean isReady() {
        return IdentityLifecycleService.isReady();
    }

    public static void onLocalPlayerCreated(int playerNum, IsoPlayer player) {
        try {
            PlayerIdentity identity = IdentityLifecycleService.resolveAndPromoteLocalPlayer(playerNum, player);
            if (identity == null) {
                return;
            }

            Map<String, Object> data = identity.toMap();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                LoaderLog.info("[PZLIFE][IDENTITY][local] " + entry.getKey() + "=" + entry.getValue());
            }
        } catch (Exception e) {
            LoaderLog.error("[PZLIFE][IDENTITY][local] Failed to resolve local player identity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void onServerPlayerReady(IsoPlayer player, String module, String command) {
        LoaderLog.info("[PZLIFE][IDENTITY][server] PlayerReady received. module=" + module + ", command=" + command);
        onLocalPlayerCreated(0, player);
    }
}
