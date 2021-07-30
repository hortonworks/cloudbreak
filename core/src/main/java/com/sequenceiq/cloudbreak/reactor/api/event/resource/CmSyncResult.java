package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;

public class CmSyncResult extends ClusterPlatformResult<CmSyncRequest> {

    private CmSyncOperationSummary cmSyncOperationSummary;

    public CmSyncResult(CmSyncRequest request, CmSyncOperationSummary cmSyncOperationSummary) {
        super(request);
        this.cmSyncOperationSummary = cmSyncOperationSummary;
    }

    public CmSyncResult(String statusReason, Exception errorDetails, CmSyncRequest request) {
        super(statusReason, errorDetails, request);
    }

}
