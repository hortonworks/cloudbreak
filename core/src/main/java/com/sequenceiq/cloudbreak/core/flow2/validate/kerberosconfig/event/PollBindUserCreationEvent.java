package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PollBindUserCreationEvent extends StackEvent {

    private final String operationId;

    private final String accountId;

    public PollBindUserCreationEvent(Long stackId, String operationId, String accountId) {
        super(stackId);
        this.operationId = operationId;
        this.accountId = accountId;
    }

    @JsonCreator
    public PollBindUserCreationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("accountId") String accountId) {
        super(selector, stackId);
        this.operationId = operationId;
        this.accountId = accountId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getAccountId() {
        return accountId;
    }
}
