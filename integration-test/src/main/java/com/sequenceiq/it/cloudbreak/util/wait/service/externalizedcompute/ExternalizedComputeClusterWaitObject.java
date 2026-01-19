package com.sequenceiq.it.cloudbreak.util.wait.service.externalizedcompute;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class ExternalizedComputeClusterWaitObject implements WaitObject {

    private ExternalizedComputeClusterClient externalizedComputeClusterClient;

    private String environmentCrn;

    private String name;

    private ExternalizedComputeClusterApiStatus desiredStatus;

    private ExternalizedComputeClusterResponse computeCluster;

    private TestContext testContext;

    public ExternalizedComputeClusterWaitObject(ExternalizedComputeClusterClient externalizedComputeClusterClient, String environmentCrn, String name,
            ExternalizedComputeClusterApiStatus desiredStatus, TestContext testContext) {
        this.externalizedComputeClusterClient = externalizedComputeClusterClient;
        this.environmentCrn = environmentCrn;
        this.name = name;
        this.desiredStatus = desiredStatus;
        this.testContext = testContext;
    }

    public ExternalizedComputeClusterEndpoint getEndpoint() {
        return externalizedComputeClusterClient.getDefaultClient(testContext).externalizedComputeClusterEndpoint();
    }

    @Override
    public void fetchData() {
        computeCluster = getEndpoint().describe(environmentCrn, name);
    }

    @Override
    public boolean isDeleteFailed() {
        return hasStatus(ExternalizedComputeClusterApiStatus.DELETE_FAILED);
    }

    @Override
    public Map<String, String> actualStatuses() {
        return Optional.ofNullable(computeCluster)
                .map(ExternalizedComputeClusterResponse::getStatus)
                .map(ExternalizedComputeClusterApiStatus::name)
                .map(status -> Map.of(STATUS, status))
                .orElse(Map.of());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        return Optional.ofNullable(computeCluster)
                .map(ExternalizedComputeClusterResponse::getStatusReason)
                .map(status -> Map.of(STATUS_REASON, status))
                .orElse(Map.of());
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
        return hasStatus(ExternalizedComputeClusterApiStatus.DELETED);
    }

    @Override
    public boolean isFailedButIgnored() {
        return false;
    }

    @Override
    public boolean isFailed() {
        return statusMatches(ExternalizedComputeClusterApiStatus::isFailed);
    }

    @Override
    public boolean isDeletionInProgress() {
        return hasStatus(ExternalizedComputeClusterApiStatus.DELETE_IN_PROGRESS);
    }

    @Override
    public boolean isCreateFailed() {
        return hasStatus(ExternalizedComputeClusterApiStatus.CREATE_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatus.equals(ExternalizedComputeClusterApiStatus.DELETED);
    }

    @Override
    public boolean isFailedCheck() {
        return desiredStatus.isFailed();
    }

    private boolean hasStatus(ExternalizedComputeClusterApiStatus status) {
        return statusMatches(apiStatus -> Objects.equals(status, apiStatus));
    }

    public boolean statusMatches(Predicate<ExternalizedComputeClusterApiStatus> statusCheck) {
        return Optional.ofNullable(computeCluster)
                .map(ExternalizedComputeClusterResponse::getStatus)
                .filter(statusCheck)
                .isPresent();
    }
}
