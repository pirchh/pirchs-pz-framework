package pirch.pz.service;

import java.lang.reflect.Method;
import zombie.characters.IsoPlayer;
import zombie.core.znet.SteamUser;

public final class PzPlayerIdentityAdapter {

    public PlayerIdentity fromIsoPlayer(IsoPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("IsoPlayer is null");
        }

        String steamId = resolveSteamId(player);
        String onlineId = safeOnlineId(player);

        String username = normalize(player.getUsername());
        String displayName = normalize(player.getDisplayName());

        String characterForename = extractCharacterForename(player);
        String characterSurname = extractCharacterSurname(player);
        String characterFullName = buildCharacterFullName(characterForename, characterSurname);

        PlayerIdentity.Builder builder = PlayerIdentity.builder()
            .playerSource(resolvePlayerSource(steamId, username, onlineId))
            .steamId(steamId)
            .onlineId(onlineId)
            .username(username)
            .displayName(displayName)
            .characterForename(characterForename)
            .characterSurname(characterSurname)
            .characterFullName(characterFullName);

        if (steamId != null) {
            builder.sourcePlayerId(steamId);
        } else if (username != null) {
            builder.sourcePlayerId(username);
        } else if (onlineId != null) {
            builder.sourcePlayerId(onlineId);
        }

        return builder.build();
    }

    private String resolveSteamId(IsoPlayer player) {
        String fromPlayer = safeSteamId(player);
        if (fromPlayer != null) {
            return fromPlayer;
        }

        String fromSteamUser = safeSteamUserId();
        if (fromSteamUser != null) {
            return fromSteamUser;
        }

        return null;
    }

    private String safeSteamId(IsoPlayer player) {
        try {
            long steamId = player.getSteamID();
            if (steamId <= 0L) {
                return null;
            }
            return Long.toUnsignedString(steamId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safeSteamUserId() {
        try {
            String steamId = normalize(SteamUser.GetSteamIDString());
            if (steamId == null || "0".equals(steamId)) {
                return null;
            }
            return steamId;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safeOnlineId(IsoPlayer player) {
        try {
            short onlineId = player.getOnlineID();
            if (onlineId < 0) {
                return null;
            }
            return Short.toString(onlineId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolvePlayerSource(String steamId, String username, String onlineId) {
        if (steamId != null) {
            return "steam";
        }
        if (username != null) {
            return "username";
        }
        if (onlineId != null) {
            return "online";
        }
        return "unknown";
    }

    private String extractCharacterForename(IsoPlayer player) {
        Object descriptor = invokeNoArgs(player, "getDescriptor");
        if (descriptor != null) {
            String value = firstNonBlank(
                invokeStringNoArgs(descriptor, "getForename"),
                invokeStringNoArgs(descriptor, "getFirstName"),
                invokeStringNoArgs(descriptor, "getName")
            );
            if (value != null) {
                return value;
            }
        }

        return firstNonBlank(
            invokeStringNoArgs(player, "getForename"),
            invokeStringNoArgs(player, "getFirstName")
        );
    }

    private String extractCharacterSurname(IsoPlayer player) {
        Object descriptor = invokeNoArgs(player, "getDescriptor");
        if (descriptor != null) {
            String value = firstNonBlank(
                invokeStringNoArgs(descriptor, "getSurname"),
                invokeStringNoArgs(descriptor, "getLastName")
            );
            if (value != null) {
                return value;
            }
        }

        return firstNonBlank(
            invokeStringNoArgs(player, "getSurname"),
            invokeStringNoArgs(player, "getLastName")
        );
    }

    private String buildCharacterFullName(String forename, String surname) {
        String joined = ((forename == null ? "" : forename) + " " + (surname == null ? "" : surname)).trim();
        return normalize(joined);
    }

    private Object invokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String invokeStringNoArgs(Object target, String methodName) {
        Object value = invokeNoArgs(target, methodName);
        if (value == null) {
            return null;
        }
        return normalize(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}