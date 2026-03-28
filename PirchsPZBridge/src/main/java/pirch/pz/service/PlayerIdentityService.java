package pirch.pz.service;

import java.util.Map;

public final class PlayerIdentityService {
    private PlayerIdentityService() {
    }

    public static PlayerIdentity fromBridgeArg(Object arg) {
        if (arg == null) {
            throw new IllegalArgumentException("player identity argument is required");
        }
        if (arg instanceof PlayerIdentity playerIdentity) {
            return validate(playerIdentity);
        }
        if (arg instanceof Map<?, ?> map) {
            return validate(fromMap(map));
        }
        String legacyId = String.valueOf(arg).trim();
        if (legacyId.isEmpty()) {
            throw new IllegalArgumentException("player identity argument cannot be empty");
        }
        return validate(PlayerIdentity.legacy(legacyId));
    }

    public static PlayerIdentity fromMap(Map<?, ?> map) {
        return PlayerIdentity.builder()
            .playerSource(stringValue(map.get("playerSource"), map.get("source")))
            .sourcePlayerId(stringValue(map.get("sourcePlayerId"), map.get("playerId"), map.get("onlineId")))
            .steamId(stringValue(map.get("steamId")))
            .onlineId(stringValue(map.get("onlineId")))
            .username(stringValue(map.get("username"), map.get("name")))
            .displayName(stringValue(map.get("displayName"), map.get("screenName")))
            .characterForename(stringValue(map.get("characterForename"), map.get("forename"), map.get("firstName")))
            .characterSurname(stringValue(map.get("characterSurname"), map.get("surname"), map.get("lastName")))
            .characterFullName(stringValue(map.get("characterFullName"), map.get("characterName")))
            .build();
    }

    public static PlayerIdentity requireResolvedLocalIdentity() {
        if (!IdentityLifecycleService.isReady()) {
            throw new IllegalStateException("identity lifecycle is not ready");
        }

        if (!IdentityLifecycleService.hasResolvedLocalAccount()) {
            throw new IllegalStateException("local account has not been resolved yet");
        }

        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        if (identity == null) {
            throw new IllegalStateException("local identity is missing from lifecycle state");
        }

        return validate(identity);
    }

    public static PlayerIdentity getResolvedLocalIdentity() {
        if (!IdentityLifecycleService.isReady() || !IdentityLifecycleService.hasResolvedLocalAccount()) {
            return null;
        }

        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        if (identity == null) {
            return null;
        }

        return validate(identity);
    }

    public static Integer requireResolvedLocalAccountId() {
        if (!IdentityLifecycleService.isReady()) {
            throw new IllegalStateException("identity lifecycle is not ready");
        }

        Integer accountId = IdentityLifecycleService.getLastResolvedAccountId();
        if (accountId == null) {
            throw new IllegalStateException("local account id has not been resolved yet");
        }

        return accountId;
    }

    public static PlayerIdentity validate(PlayerIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("player identity cannot be null");
        }
        identity.getAccountExternalId();
        identity.getCharacterExternalId();
        return identity;
    }

    private static String stringValue(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = String.valueOf(value).trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }
}