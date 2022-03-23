package com.sequenceiq.cloudbreak.service.freeipa;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;

public class FreeIpaOperationPollerObject {
    private final String operationId;

    private final String operationType;

    private final String accountId;

    private final OperationV1Endpoint operationV1Endpoint;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public FreeIpaOperationPollerObject(String operationId, String operationType, OperationV1Endpoint operationV1Endpoint,
        String accountId, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.operationV1Endpoint = operationV1Endpoint;
        this.accountId = accountId;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public String getOperationId() {
        return operationId;
    }

    public OperationV1Endpoint getOperationV1Endpoint() {
        return operationV1Endpoint;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getAccountId() {
        return accountId;
    }

    public RegionAwareInternalCrnGeneratorFactory getRegionalAwareInternalCrnGeneratorFactory() {
        return regionAwareInternalCrnGeneratorFactory;
    }
}
