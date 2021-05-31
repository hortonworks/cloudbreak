package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PollBindUserCreationEvent extends StackEvent {

    private final String operationId;

    private final String accountId;

    public PollBindUserCreationEvent(Long stackId, String operationId, String accountId) {
        super(stackId);
        this.operationId = operationId;
        this.accountId = accountId;
    }

    public PollBindUserCreationEvent(String selector, Long stackId, String operationId, String accountId) {
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
