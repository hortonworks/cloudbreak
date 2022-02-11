package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Objects;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeCcmTriggerEvent extends StackEvent {

    private final String accountId;

    private final String operationId;

    private final String environmentCrn;

    public UpgradeCcmTriggerEvent(String selector, String accountId, String operationId, String environmentCrn, Long stackId) {
        super(selector, stackId);
        this.accountId = accountId;
        this.operationId = operationId;
        this.environmentCrn = environmentCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(UpgradeCcmTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(accountId, event.accountId)
                        && Objects.equals(environmentCrn, event.environmentCrn));
    }

    @Override
    public String toString() {
        return "UpgradeCcmTriggerEvent{" +
                "accountId='" + accountId + '\'' +
                ", operationId='" + operationId + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                "} " + super.toString();
    }

}
