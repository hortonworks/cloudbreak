package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CreateBindUserEvent extends StackEvent {

    private final String accountId;

    private final String operationId;

    private final String suffix;

    private final String environmentCrn;

    public CreateBindUserEvent(Long stackId) {
        super(stackId);
        accountId = null;
        operationId = null;
        suffix = null;
        environmentCrn = null;
    }

    public CreateBindUserEvent(String selector, Long stackId) {
        super(selector, stackId);
        accountId = null;
        operationId = null;
        suffix = null;
        environmentCrn = null;
    }

    @JsonCreator
    public CreateBindUserEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("suffix") String suffix,
            @JsonProperty("environmentCrn") String environmentCrn) {
        super(selector, stackId);
        this.accountId = accountId;
        this.operationId = operationId;
        this.suffix = suffix;
        this.environmentCrn = environmentCrn;
    }

    public CreateBindUserEvent(String selector, CreateBindUserEvent event) {
        super(selector, event.getResourceId());
        accountId = event.getAccountId();
        operationId = event.getOperationId();
        suffix = event.getSuffix();
        environmentCrn = event.getEnvironmentCrn();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    private CreateBindUserEvent getEvent() {
        return this;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(CreateBindUserEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(accountId, event.accountId)
                        && Objects.equals(suffix, event.suffix)
                        && Objects.equals(environmentCrn, event.environmentCrn));
    }

    @Override
    public String toString() {
        return super.toString() + ' ' +
                "CreateBindUserEvent{" +
                "accountId='" + accountId + '\'' +
                ", operationId='" + operationId + '\'' +
                ", suffix='" + suffix + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
