package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class EnvironmentWaitObject implements WaitObject {

    private final EnvironmentClient client;

    private final String crn;

    private final EnvironmentStatus desiredStatus;

    private final Set<EnvironmentStatus> ignoredFailedStatuses;

    private DetailedEnvironmentResponse environment;

    private final String name;

    private final TestContext testContext;

    public EnvironmentWaitObject(EnvironmentClient environmentClient, String name, String environmentCrn, EnvironmentStatus desiredStatus,
            Set<EnvironmentStatus> ignoredFailedStatuses, TestContext testContext) {
        this.client = environmentClient;
        this.crn = environmentCrn;
        this.name = name;
        this.desiredStatus = desiredStatus;
        this.ignoredFailedStatuses = ignoredFailedStatuses;
        this.testContext = testContext;
    }

    public EnvironmentEndpoint getEndpoint() {
        return client.getDefaultClient(testContext).environmentV1Endpoint();
    }

    public String getCrn() {
        return crn;
    }

    public EnvironmentStatus getDesiredStatus() {
        return desiredStatus;
    }

    @Override
    public void fetchData() {
        environment = getEndpoint().getByCrn(crn);
    }

    @Override
    public boolean isDeleteFailed() {
        return environment.getEnvironmentStatus().equals(DELETE_FAILED);
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (environment == null || environment.getEnvironmentStatus() == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, environment.getEnvironmentStatus().name());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        String statusReason = environment.getStatusReason();
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
        return environment.getEnvironmentStatus().equals(ARCHIVED);
    }

    @Override
    public boolean isFailedButIgnored() {
        return ignoredFailedStatuses.contains(environment.getEnvironmentStatus());
    }

    @Override
    public boolean isFailed() {
        return environment.getEnvironmentStatus().isFailed();
    }

    @Override
    public boolean isDeletionInProgress() {
        return environment.getEnvironmentStatus().isDeleteInProgress();
    }

    @Override
    public boolean isCreateFailed() {
        return environment.getEnvironmentStatus().equals(CREATE_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatus.equals(ARCHIVED);
    }

    @Override
    public boolean isFailedCheck() {
        return desiredStatus.isFailed();
    }
}
