package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DATALAKE_UPGRADE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.PROVISIONING_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.REPAIR_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.START_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOP_FAILED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.SYNC_FAILED;

import java.util.Set;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeWaitObject {

    private final SdxClient client;

    private final String name;

    private final SdxClusterStatusResponse desiredStatus;

    public DatalakeWaitObject(SdxClient client, String name, SdxClusterStatusResponse desiredStatus) {
        this.client = client;
        this.name = name;
        this.desiredStatus = desiredStatus;
    }

    public SdxEndpoint getEndpoint() {
        return client.getSdxClient().sdxEndpoint();
    }

    public String getName() {
        return name;
    }

    public SdxClusterStatusResponse getDesiredStatus() {
        return desiredStatus;
    }

    public boolean isFailed(SdxClusterStatusResponse datalakeStatus) {
        Set<SdxClusterStatusResponse> failedStatuses = Set.of(PROVISIONING_FAILED, REPAIR_FAILED, DATALAKE_UPGRADE_FAILED,
                DELETE_FAILED, START_FAILED, STOP_FAILED, SYNC_FAILED);
        return failedStatuses.contains(datalakeStatus);
    }
}
