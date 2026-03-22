package pirch.pz.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PlayerIdentity {
    private final String playerSource;
    private final String sourcePlayerId;
    private final String steamId;
    private final String onlineId;
    private final String username;
    private final String displayName;
    private final String characterForename;
    private final String characterSurname;
    private final String characterFullName;
    private final String accountExternalId;
    private final String characterExternalId;

    private PlayerIdentity(Builder builder) {
        this.playerSource = normalize(builder.playerSource);
        this.sourcePlayerId = normalize(builder.sourcePlayerId);
        this.steamId = normalize(builder.steamId);
        this.onlineId = normalize(builder.onlineId);
        this.username = normalize(builder.username);
        this.displayName = normalize(builder.displayName);
        this.characterForename = normalize(builder.characterForename);
        this.characterSurname = normalize(builder.characterSurname);
        this.characterFullName = normalize(builder.characterFullName);
        this.accountExternalId = buildAccountExternalId();
        this.characterExternalId = buildCharacterExternalId();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PlayerIdentity legacy(String externalId) {
        return builder()
            .playerSource("legacy")
            .sourcePlayerId(externalId)
            .username(externalId)
            .displayName(externalId)
            .build();
    }

    public String getPlayerSource() {
        return playerSource;
    }

    public String getSourcePlayerId() {
        return sourcePlayerId;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getOnlineId() {
        return onlineId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCharacterForename() {
        return characterForename;
    }

    public String getCharacterSurname() {
        return characterSurname;
    }

    public String getCharacterFullName() {
        return characterFullName;
    }

    public String getAccountExternalId() {
        return accountExternalId;
    }

    public String getCharacterExternalId() {
        return characterExternalId;
    }

    public String getCanonicalExternalId() {
        return accountExternalId;
    }

    public String getPreferredAccountName() {
        if (displayName != null) {
            return displayName;
        }
        if (username != null) {
            return username;
        }
        return accountExternalId;
    }

    public String getPreferredCharacterName() {
        if (characterFullName != null) {
            return characterFullName;
        }
        if (characterForename != null) {
            return characterForename;
        }
        return getPreferredAccountName();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("playerSource", playerSource);
        data.put("sourcePlayerId", sourcePlayerId);
        data.put("steamId", steamId);
        data.put("onlineId", onlineId);
        data.put("username", username);
        data.put("displayName", displayName);
        data.put("characterForename", characterForename);
        data.put("characterSurname", characterSurname);
        data.put("characterFullName", characterFullName);
        data.put("accountExternalId", accountExternalId);
        data.put("characterExternalId", characterExternalId);
        data.put("canonicalExternalId", accountExternalId);
        data.put("preferredAccountName", getPreferredAccountName());
        data.put("preferredCharacterName", getPreferredCharacterName());
        return data;
    }

    private String buildAccountExternalId() {
        if (steamId != null) {
            return "steam:" + steamId;
        }
        if (username != null) {
            return "user:" + username.toLowerCase(Locale.ROOT);
        }
        if (onlineId != null) {
            return "online:" + onlineId;
        }
        if (playerSource != null && sourcePlayerId != null) {
            return playerSource + ":" + sourcePlayerId;
        }
        throw new IllegalStateException("PlayerIdentity must contain at least one usable identifier");
    }

    private String buildCharacterExternalId() {
        String characterName = slugify(getPreferredCharacterName());
        if (characterName == null) {
            return accountExternalId;
        }
        return accountExternalId + "#char:" + characterName;
    }

    private static String slugify(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        String slug = normalized
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? null : slug;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerIdentity other)) {
            return false;
        }
        return Objects.equals(accountExternalId, other.accountExternalId)
            && Objects.equals(characterExternalId, other.characterExternalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountExternalId, characterExternalId);
    }

    public static final class Builder {
        private String playerSource;
        private String sourcePlayerId;
        private String steamId;
        private String onlineId;
        private String username;
        private String displayName;
        private String characterForename;
        private String characterSurname;
        private String characterFullName;

        private Builder() {
        }

        public Builder playerSource(String playerSource) {
            this.playerSource = playerSource;
            return this;
        }

        public Builder sourcePlayerId(String sourcePlayerId) {
            this.sourcePlayerId = sourcePlayerId;
            return this;
        }

        public Builder steamId(String steamId) {
            this.steamId = steamId;
            return this;
        }

        public Builder onlineId(String onlineId) {
            this.onlineId = onlineId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder characterForename(String characterForename) {
            this.characterForename = characterForename;
            return this;
        }

        public Builder characterSurname(String characterSurname) {
            this.characterSurname = characterSurname;
            return this;
        }

        public Builder characterFullName(String characterFullName) {
            this.characterFullName = characterFullName;
            return this;
        }

        public PlayerIdentity build() {
            return new PlayerIdentity(this);
        }
    }
}
