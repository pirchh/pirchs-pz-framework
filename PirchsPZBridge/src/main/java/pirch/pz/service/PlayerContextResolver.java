package pirch.pz.service;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoPlayer;

public final class PlayerContextResolver {

    public IsoPlayer resolveLocalPlayer() {
        IsoPlayer player = tryResolveAnyPlayer();
        if (player != null) {
            return player;
        }

        throw new IllegalStateException("No local IsoPlayer is available yet.");
    }

    public IsoPlayer resolveFromOnlineId(short onlineId) {
        IsoPlayer player = IsoPlayer.getLocalPlayerByOnlineID(onlineId);
        if (player != null) {
            return player;
        }

        throw new IllegalStateException("No IsoPlayer found for onlineId=" + onlineId);
    }

    public PlayerIdentity resolveLocalIdentity() {
        IsoPlayer player = resolveLocalPlayer();
        return new PzPlayerIdentityAdapter().fromIsoPlayer(player);
    }

    public PlayerIdentity resolveIdentityFromOnlineId(short onlineId) {
        IsoPlayer player = resolveFromOnlineId(onlineId);
        return new PzPlayerIdentityAdapter().fromIsoPlayer(player);
    }

    public IsoPlayer tryResolveAnyPlayer() {
        IsoPlayer instance = tryGetInstance();
        if (instance != null) {
            return instance;
        }

        List<IsoPlayer> players = tryGetPlayers();
        if (players != null) {
            for (IsoPlayer entry : players) {
                if (entry != null && safeIsLocalPlayer(entry)) {
                    return entry;
                }
            }

            for (IsoPlayer entry : players) {
                if (entry != null) {
                    return entry;
                }
            }
        }

        return null;
    }

    private IsoPlayer tryGetInstance() {
        try {
            return IsoPlayer.getInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<IsoPlayer> tryGetPlayers() {
        try {
            ArrayList<IsoPlayer> players = IsoPlayer.getPlayers();
            return players;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean safeIsLocalPlayer(IsoPlayer player) {
        try {
            return player.isLocalPlayer();
        } catch (Exception ignored) {
            return false;
        }
    }
}
