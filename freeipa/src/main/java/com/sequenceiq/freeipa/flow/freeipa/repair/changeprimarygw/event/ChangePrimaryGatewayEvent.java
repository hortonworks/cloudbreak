package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event;

import java.util.List;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class ChangePrimaryGatewayEvent extends StackEvent {
    private final String operationId;

    private final Boolean finalChain;

    private final List<String> repairInstanceIds;

    public ChangePrimaryGatewayEvent(String selector, Long stackId, List<String> repairInstanceIds, Boolean finalChain, String operationId) {
        super(selector, stackId);
        this.repairInstanceIds = repairInstanceIds;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public ChangePrimaryGatewayEvent(String selector, Long stackId, List<String> repairInstanceIds, Boolean finalChain, String operationId,
            Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.repairInstanceIds = repairInstanceIds;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public List<String> getRepairInstaceIds() {
        return repairInstanceIds;
    }

    public Boolean isFinalChain() {
        return finalChain;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "ChangePrimaryGatewayEvent{"
                + "stackId=" + getResourceId()
                + ", repairInstanceIds='" + repairInstanceIds + '\''
                + ", finalChain='" + finalChain + '\''
                + ", operationId='" + operationId + '\''
                + "}";
    }
}
