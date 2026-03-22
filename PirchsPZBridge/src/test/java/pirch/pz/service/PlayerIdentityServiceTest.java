package pirch.pz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerIdentityServiceTest {
    @Test
    void prefersSteamAsAccountIdentity() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerSource", "steam");
        payload.put("sourcePlayerId", "76561198000000000");
        payload.put("steamId", "76561198000000000");
        payload.put("username", "Charlie");
        payload.put("displayName", "Charlie Ryan");
        payload.put("characterFullName", "Frank West");

        PlayerIdentity identity = PlayerIdentityService.fromMap(payload);

        assertEquals("steam:76561198000000000", identity.getAccountExternalId());
        assertEquals("steam:76561198000000000#char:frank-west", identity.getCharacterExternalId());
    }

    @Test
    void fallsBackToUsernameWhenNoSteamIdExists() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerSource", "username");
        payload.put("username", "SomeUser");
        payload.put("displayName", "Some User");

        PlayerIdentity identity = PlayerIdentityService.fromMap(payload);

        assertEquals("user:someuser", identity.getAccountExternalId());
        assertTrue(identity.getCharacterExternalId().startsWith("user:someuser"));
    }

    @Test
    void legacyInputStillWorks() {
        PlayerIdentity identity = PlayerIdentityService.fromBridgeArg("legacy-user");

        assertEquals("user:legacy-user", identity.getAccountExternalId());
    }
}
