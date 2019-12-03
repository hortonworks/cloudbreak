package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Objects;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CcmKeyDeregisterRequest extends StackEvent {

    private final String actorCrn;

    private final String accountId;

    private final String keyId;

    private final Boolean useCcm;

    public CcmKeyDeregisterRequest(Long stackId, String actorCrn, String accountId, String keyId, Boolean useCcm) {
        super(stackId);
        this.actorCrn = Objects.requireNonNull(actorCrn, "actorCrn is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.keyId = Objects.requireNonNull(keyId, "keyId is null");
        this.useCcm = useCcm;
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

    public Boolean getUseCcm() {
        return useCcm;
    }
}
