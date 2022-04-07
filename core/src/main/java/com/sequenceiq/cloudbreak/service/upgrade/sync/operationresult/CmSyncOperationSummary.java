package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Objects;

public class CmSyncOperationSummary {

    private CmSyncOperationStatus syncOperationStatus;

    private CmSyncOperationResult syncOperationResult;

    public CmSyncOperationSummary(CmSyncOperationStatus syncOperationStatus, CmSyncOperationResult syncOperationResult) {
        this.syncOperationStatus = syncOperationStatus;
        this.syncOperationResult = syncOperationResult;
    }

    public CmSyncOperationSummary(CmSyncOperationStatus syncOperationStatus) {
        this.syncOperationStatus = syncOperationStatus;
    }

    public CmSyncOperationStatus getSyncOperationStatus() {
        return syncOperationStatus;
    }

    public void setSyncOperationStatus(CmSyncOperationStatus syncOperationStatus) {
        this.syncOperationStatus = syncOperationStatus;
    }

    public CmSyncOperationResult getSyncOperationResult() {
        return syncOperationResult;
    }

    public void setSyncOperationResult(CmSyncOperationResult syncOperationResult) {
        this.syncOperationResult = syncOperationResult;
    }

    public boolean hasResult() {
        return Objects.nonNull(syncOperationResult);
    }

    @Override
    public String toString() {
        return "CmSyncOperationSummary{" +
                "syncOperationStatus=" + syncOperationStatus +
                ", syncOperationResult=" + syncOperationResult +
                '}';
    }
}
