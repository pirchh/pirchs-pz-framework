package pirch.pz.service;

import java.util.LinkedHashMap;
import java.util.Map;

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

    public String getCanonicalExternalId() {
        if (steamId != null && !steamId.isBlank()) {
            return steamId;
        }
        if (username != null && !username.isBlank()) {
            return "user:" + username;
        }
        if (onlineId != null && !onlineId.isBlank()) {
            return "online:" + onlineId;
        }
        if (playerSource != null && sourcePlayerId != null) {
            return playerSource + ":" + sourcePlayerId;
        }
        throw new IllegalStateException("PlayerIdentity must contain at least one usable identifier");
    }

    public String getPreferredAccountName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return getCanonicalExternalId();
    }

    public String getPreferredCharacterName() {
        if (characterFullName != null && !characterFullName.isBlank()) {
            return characterFullName;
        }
        if (characterForename != null && !characterForename.isBlank()) {
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
        data.put("canonicalExternalId", getCanonicalExternalId());
        data.put("preferredAccountName", getPreferredAccountName());
        data.put("preferredCharacterName", getPreferredCharacterName());
        return data;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
