package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaOperationWaitObject extends FreeIpaWaitObject {

    private final String operationId;

    private final OperationState desiredOperationState;

    private final Set<OperationState> ignoredFailedStatuses;

    private OperationStatus operationStatus;

    private TestContext testContext;

    public FreeIpaOperationWaitObject(FreeIpaClient freeIpaClient, String operationId, String freeipaName, String environmentCrn,
            OperationState desiredOperationState, Set<OperationState> ignoredFailedStatuses, TestContext testContext) {
        super(freeIpaClient, freeipaName, environmentCrn, Status.AVAILABLE, testContext);
        this.operationId = operationId;
        this.desiredOperationState = desiredOperationState;
        this.ignoredFailedStatuses = ignoredFailedStatuses;
        this.testContext = testContext;
    }

    @Override
    public void fetchData() {
        super.fetchData();
        if (operationId != null) {
            operationStatus = getClient().getDefaultClient(testContext).getOperationV1Endpoint()
                    .getOperationStatus(operationId, Crn.safeFromString(getEnvironmentCrn()).getAccountId());
        }
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (operationStatus != null) {
            return Map.of(STATUS, operationStatus.getStatus().name());
        } else {
            return Map.of();
        }
    }

    @Override
    public Map<String, String> actualStatusReason() {
        String operationStatusError = operationStatus.getError();
        if (operationStatusError != null) {
            return Map.of(STATUS_REASON, operationStatusError);
        }
        return Map.of();
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        return Map.of(STATUS, desiredOperationState.name());
    }

    @Override
    public boolean isFailedButIgnored() {
        return ignoredFailedStatuses.contains(operationStatus.getStatus());
    }

    @Override
    public String getName() {
        return super.getEnvironmentCrn() + " - operationID: " + operationId;
    }

    @Override
    public boolean isFailed() {
        return operationStatus != null && (OperationState.FAILED == operationStatus.getStatus()
                || OperationState.REJECTED == operationStatus.getStatus()
                || OperationState.TIMEDOUT == operationStatus.getStatus());
    }
}