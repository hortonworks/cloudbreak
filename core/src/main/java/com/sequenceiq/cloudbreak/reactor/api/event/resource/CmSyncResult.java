package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class CmSyncResult extends ClusterPlatformResult<CmSyncRequest> {

    public CmSyncResult(CmSyncRequest request) {
        super(request);
    }

    public CmSyncResult(String statusReason, Exception errorDetails, CmSyncRequest request) {
        super(statusReason, errorDetails, request);
    }

}
