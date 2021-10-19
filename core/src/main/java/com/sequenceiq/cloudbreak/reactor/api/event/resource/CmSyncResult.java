package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class CmSyncResult extends ClusterPlatformResult<CmSyncRequest> {

    private String result;

    public CmSyncResult(CmSyncRequest request, String result) {
        super(request);
        this.result = result;
    }

    public CmSyncResult(String statusReason, Exception errorDetails, CmSyncRequest request) {
        super(statusReason, errorDetails, request);
    }

    public String getResult() {
        return result;
    }
}
