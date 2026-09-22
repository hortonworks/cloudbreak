package com.sequenceiq.cloudbreak.maintenancewindow;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

/**
 * Builds {@link MaintenanceTaskDispatchRequest} instances for tests, pre-filled with a valid dispatch so each test
 * only overrides the field it exercises. Named setters keep the tests readable and immune to field reordering.
 */
public final class MaintenanceTaskDispatchRequestTestBuilder {

    public static final String ACCOUNT_ID = "acc-12345";

    public static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh";

    private final MaintenanceTaskDispatchRequest request = new MaintenanceTaskDispatchRequest();

    private MaintenanceTaskDispatchRequestTestBuilder() {
        request.setTaskId(10L);
        request.setRunId(20L);
        request.setIdempotencyKey("idem-1");
        request.setAccountId(ACCOUNT_ID);
        request.setResourceCrn(RESOURCE_CRN);
        request.setTaskType(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        request.setWorkItemId("SALT_PASSWORD");
        request.setTaskKind("ONE_SHOT");
        request.setMaintenanceScheduleId(1L);
        request.setPolicyRevision("1:v1");
        request.setWindowStart(1000L);
        request.setWindowEnd(2000L);
    }

    public static MaintenanceTaskDispatchRequestTestBuilder aDispatchRequest() {
        return new MaintenanceTaskDispatchRequestTestBuilder();
    }

    public MaintenanceTaskDispatchRequestTestBuilder withTaskType(String taskType) {
        request.setTaskType(taskType);
        return this;
    }

    public MaintenanceTaskDispatchRequestTestBuilder withAccountId(String accountId) {
        request.setAccountId(accountId);
        return this;
    }

    public MaintenanceTaskDispatchRequestTestBuilder withResourceCrn(String resourceCrn) {
        request.setResourceCrn(resourceCrn);
        return this;
    }

    public MaintenanceTaskDispatchRequestTestBuilder withTaskPayload(Map<String, Object> taskPayload) {
        request.setTaskPayload(taskPayload);
        return this;
    }

    public MaintenanceTaskDispatchRequest build() {
        return request;
    }
}
