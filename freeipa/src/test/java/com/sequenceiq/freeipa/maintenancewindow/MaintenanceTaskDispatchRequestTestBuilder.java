package com.sequenceiq.freeipa.maintenancewindow;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.freeipa.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskKind;

/**
 * Builds {@link MaintenanceTaskDispatchRequest} instances for tests, pre-filled with a valid dispatch so each test
 * only overrides the field it exercises.
 */
public final class MaintenanceTaskDispatchRequestTestBuilder {

    public static final String ACCOUNT_ID = "acc-12345";

    public static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:acc-12345:freeipa:my-ipa";

    private final MaintenanceTaskDispatchRequest request = new MaintenanceTaskDispatchRequest();

    private MaintenanceTaskDispatchRequestTestBuilder() {
        populateDefaults(request);
    }

    private static void populateDefaults(MaintenanceTaskDispatchRequest target) {
        populateRunIdentity(target);
        populateTaskFields(target);
        populateScheduleFields(target);
    }

    private static void populateRunIdentity(MaintenanceTaskDispatchRequest target) {
        target.setTaskId(10L);
        target.setRunId(20L);
        target.setIdempotencyKey("idem-1");
        target.setAccountId(ACCOUNT_ID);
    }

    private static void populateTaskFields(MaintenanceTaskDispatchRequest target) {
        target.setResourceCrn(RESOURCE_CRN);
        target.setTaskType(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        target.setWorkItemId("SALT_PASSWORD");
        target.setTaskKind(MaintenanceTaskKind.ONE_SHOT.name());
    }

    private static void populateScheduleFields(MaintenanceTaskDispatchRequest target) {
        target.setMaintenanceScheduleId(1L);
        target.setPolicyRevision("1:v1");
        target.setWindowStart(1000L);
        target.setWindowEnd(2000L);
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
