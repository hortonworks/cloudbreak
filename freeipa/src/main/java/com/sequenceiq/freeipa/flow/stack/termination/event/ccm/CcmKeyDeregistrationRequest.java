package com.sequenceiq.freeipa.flow.stack.termination.event.ccm;

import java.util.Objects;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class CcmKeyDeregistrationRequest extends TerminationEvent {

    private final String actorCrn;

    private final String accountId;

    private final String keyId;

    private final Boolean useCcm;

    private final String minaSshdServiceId;

    public CcmKeyDeregistrationRequest(Long stackId, Boolean forced, String actorCrn, String accountId, String keyId, Boolean useCcm,
            String minaSshdServiceId) {
        super(EventSelectorUtil.selector(CcmKeyDeregistrationRequest.class), stackId, forced);
        this.actorCrn = Objects.requireNonNull(actorCrn, "actorCrn is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.keyId = Objects.requireNonNull(keyId, "keyId is null");
        this.useCcm = useCcm;
        this.minaSshdServiceId = minaSshdServiceId;
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

    public String getMinaSshdServiceId() {
        return minaSshdServiceId;
    }
}
