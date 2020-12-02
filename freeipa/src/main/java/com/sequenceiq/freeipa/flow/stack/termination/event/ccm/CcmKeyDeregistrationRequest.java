package com.sequenceiq.freeipa.flow.stack.termination.event.ccm;

import java.util.Objects;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class CcmKeyDeregistrationRequest extends TerminationEvent {

    private final String actorCrn;

    private final String accountId;

    private final String keyId;

    private final Tunnel tunnel;

    private final String minaSshdServiceId;

    private final String ccmV2AgentCrn;

    @SuppressWarnings("ExecutableStatementCount")
    public CcmKeyDeregistrationRequest(Long stackId, Boolean forced, String actorCrn, String accountId, String keyId, Tunnel tunnel,
            String minaSshdServiceId, String ccmV2AgentCrn) {
        super(EventSelectorUtil.selector(CcmKeyDeregistrationRequest.class), stackId, forced);
        this.actorCrn = Objects.requireNonNull(actorCrn, "actorCrn is null");
        this.accountId = Objects.requireNonNull(accountId, "accountId is null");
        this.keyId = Objects.requireNonNull(keyId, "keyId is null");
        this.tunnel = tunnel;
        this.minaSshdServiceId = minaSshdServiceId;
        this.ccmV2AgentCrn = ccmV2AgentCrn;
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

    public String getMinaSshdServiceId() {
        return minaSshdServiceId;
    }

    public String getCcmV2AgentCrn() {
        return ccmV2AgentCrn;
    }
}
