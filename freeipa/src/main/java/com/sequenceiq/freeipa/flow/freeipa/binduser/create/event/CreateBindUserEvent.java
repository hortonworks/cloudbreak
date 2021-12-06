package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import java.util.Objects;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CreateBindUserEvent extends StackEvent {

    private String accountId;

    private String operationId;

    private String suffix;

    private String environmentCrn;

    public CreateBindUserEvent(Long stackId) {
        super(stackId);
    }

    public CreateBindUserEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public CreateBindUserEvent(String selector, Long stackId, String accountId, String operationId, String suffix, String environmentCrn) {
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
