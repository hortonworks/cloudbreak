package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.CERT_ROTATION_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DATALAKE_RESTORE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DATALAKE_UPGRADE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_REQUESTED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.PROVISIONING_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.REPAIR_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.START_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOP_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.SYNC_FAILED;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeWaitObject implements WaitObject {

    private final SdxClient client;

    private final String name;

    private final SdxClusterStatusResponse desiredStatus;

    private SdxClusterResponse sdxResponse;

    public DatalakeWaitObject(SdxClient client, String name, SdxClusterStatusResponse desiredStatus) {
        this.client = client;
        this.name = name;
        this.desiredStatus = desiredStatus;
    }

    public SdxEndpoint getEndpoint() {
        return client.getDefaultClient().sdxEndpoint();
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
    public boolean isFailed() {
        Set<SdxClusterStatusResponse> failedStatuses = Set.of(PROVISIONING_FAILED, REPAIR_FAILED, DATALAKE_UPGRADE_FAILED,
                DELETE_FAILED, START_FAILED, STOP_FAILED, SYNC_FAILED, CERT_ROTATION_FAILED, DATALAKE_RESTORE_FAILED);
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
