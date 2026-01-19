package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_FAILED;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class FreeIpaWaitObject implements WaitObject {

    private final FreeIpaClient client;

    private final String environmentCrn;

    private final Status desiredStatus;

    private final Set<Status> ignoredFailedStatuses;

    private final String name;

    private DescribeFreeIpaResponse freeIpa;

    private TestContext testContext;

    public FreeIpaWaitObject(FreeIpaClient freeIpaClient, String name, String environmentCrn, Status desiredStatus, Set<Status> ignoredFailedStatuses,
            TestContext testContext) {
        this.client = freeIpaClient;
        this.environmentCrn = environmentCrn;
        this.desiredStatus = desiredStatus;
        this.ignoredFailedStatuses = ignoredFailedStatuses;
        this.name = name;
        this.testContext = testContext;
    }

    protected FreeIpaWaitObject(FreeIpaClient freeIpaClient, String name, String environmentCrn, Status desiredStatus, TestContext testContext) {
        this(freeIpaClient, name, environmentCrn, desiredStatus, Set.of(), testContext);
    }

    public FreeIpaV1Endpoint getEndpoint() {
        return client.getDefaultClient(testContext).getFreeIpaV1Endpoint();
    }

    protected FreeIpaClient getClient() {
        return client;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Status getDesiredStatus() {
        return desiredStatus;
    }

    @Override
    public void fetchData() {
        freeIpa = getEndpoint().describe(environmentCrn);
    }

    @Override
    public boolean isDeleteFailed() {
        return freeIpa.getStatus().equals(DELETE_FAILED);
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (freeIpa == null || freeIpa.getStatus() == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, freeIpa.getStatus().name());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        String statusReason = freeIpa.getStatusReason();
        if (statusReason != null) {
            return Map.of(STATUS_REASON, statusReason);
        }
        return Map.of();
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        return Map.of(STATUS, desiredStatus.name());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDeleted() {
        return freeIpa.getStatus().isSuccessfullyDeleted();
    }

    @Override
    public boolean isFailedButIgnored() {
        return ignoredFailedStatuses.contains(freeIpa.getStatus());
    }

    @Override
    public boolean isFailed() {
        return freeIpa.getStatus().isFailed();
    }

    @Override
    public boolean isDeletionInProgress() {
        return freeIpa.getStatus().isDeletionInProgress();
    }

    @Override
    public boolean isCreateFailed() {
        return freeIpa.getStatus().equals(CREATE_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatus.equals(DELETE_COMPLETED);
    }

    @Override
    public boolean isFailedCheck() {
        return desiredStatus.equals(CREATE_FAILED);
    }
}
