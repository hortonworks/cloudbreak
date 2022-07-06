package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.Tunnel;

public class CcmKeyDeregisterRequest extends StackEvent {

    private final String actorCrn;

    private final String accountId;

    private final String keyId;

    private final Tunnel tunnel;

    @JsonCreator
    public CcmKeyDeregisterRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("actorCrn") String actorCrn,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("keyId") String keyId,
            @JsonProperty("tunnel") Tunnel tunnel) {
        super(stackId);
        this.actorCrn = Objects.requireNonNull(actorCrn, "actorCrn is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.keyId = Objects.requireNonNull(keyId, "keyId is null");
        this.tunnel = tunnel;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getKeyId() {
        return keyId;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }
}
