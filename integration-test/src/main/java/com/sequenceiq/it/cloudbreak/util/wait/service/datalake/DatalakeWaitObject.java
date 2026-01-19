package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.CERT_RENEWAL_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.CERT_ROTATION_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.CLUSTER_UNREACHABLE;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DATALAKE_RESTORE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DATALAKE_UPGRADE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_REQUESTED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.NODE_FAILURE;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.PROVISIONING_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.RECOVERY_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.REPAIR_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.START_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOP_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.SYNC_FAILED;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeWaitObject implements WaitObject {

    private final SdxClient client;

    private final String name;

    private final SdxClusterStatusResponse desiredStatus;

    private final Set<SdxClusterStatusResponse> ignoredFailedStatuses;

    private SdxClusterResponse sdxResponse;

    private TestContext testContext;

    public DatalakeWaitObject(SdxClient client, String name, SdxClusterStatusResponse desiredStatus, Set<SdxClusterStatusResponse> ignoredFailedStatuses,
            TestContext testContext) {
        this.client = client;
        this.name = name;
        this.desiredStatus = desiredStatus;
        this.ignoredFailedStatuses = ignoredFailedStatuses;
        this.testContext = testContext;
    }

    public SdxEndpoint getEndpoint() {
        return client.getDefaultClient(testContext).sdxEndpoint();
    }

    @Override
    public void fetchData() {
        sdxResponse = getEndpoint().get(name);
    }

    @Override
    public boolean isDeleteFailed() {
        return sdxResponse.getStatus().equals(DELETE_FAILED);
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (sdxResponse == null || sdxResponse.getStatus() == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, sdxResponse.getStatus().name());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        String statusReason = sdxResponse.getStatusReason();
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
        return sdxResponse.getStatus().equals(DELETED);
    }

    @Override
    public boolean isFailedButIgnored() {
        return ignoredFailedStatuses.contains(sdxResponse.getStatus());
    }

    @Override
    public boolean isFailed() {
        Set<SdxClusterStatusResponse> failedStatuses = Set.of(PROVISIONING_FAILED, REPAIR_FAILED, DATALAKE_UPGRADE_FAILED,
                RECOVERY_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED, CLUSTER_UNREACHABLE, NODE_FAILURE, SYNC_FAILED,
                CERT_ROTATION_FAILED, CERT_RENEWAL_FAILED, DATALAKE_RESTORE_FAILED);
        return failedStatuses.contains(sdxResponse.getStatus());
    }

    @Override
    public boolean isDeletionInProgress() {
        Set<SdxClusterStatusResponse> deleteInProgressStatuses = Set.of(DELETE_REQUESTED, STACK_DELETION_IN_PROGRESS,
                EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
        return deleteInProgressStatuses.contains(sdxResponse.getStatus());
    }

    @Override
    public boolean isCreateFailed() {
        return sdxResponse.getStatus().equals(PROVISIONING_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatus.equals(DELETED);
    }

    @Override
    public boolean isFailedCheck() {
        return desiredStatus.equals(PROVISIONING_FAILED);
    }
}
