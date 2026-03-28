package pirch.pz.service;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IdentityLifecycleState {
    private final boolean ready;
    private final boolean resolved;
    private final Integer lastResolvedAccountId;
    private final PlayerIdentity lastIdentity;
    private final String lastResolutionSource;
    private final boolean lastResolutionAuthoritative;

    public IdentityLifecycleState(
        boolean ready,
        boolean resolved,
        Integer lastResolvedAccountId,
        PlayerIdentity lastIdentity
    ) {
        this(
            ready,
            resolved,
            lastResolvedAccountId,
            lastIdentity,
            null,
            false
        );
    }

    public IdentityLifecycleState(
        boolean ready,
        boolean resolved,
        Integer lastResolvedAccountId,
        PlayerIdentity lastIdentity,
        String lastResolutionSource,
        boolean lastResolutionAuthoritative
    ) {
        this.ready = ready;
        this.resolved = resolved;
        this.lastResolvedAccountId = lastResolvedAccountId;
        this.lastIdentity = lastIdentity;
        this.lastResolutionSource = lastResolutionSource;
        this.lastResolutionAuthoritative = lastResolutionAuthoritative;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ready", ready);
        data.put("resolved", resolved);
        data.put("lastResolvedAccountId", lastResolvedAccountId);
        data.put("lastIdentity", lastIdentity == null ? null : lastIdentity.toMap());
        data.put("lastResolutionSource", lastResolutionSource);
        data.put("lastResolutionAuthoritative", lastResolutionAuthoritative);
        return data;
    }
}