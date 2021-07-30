package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

public class CmSyncOperationResult {

    private final CmRepoSyncOperationResult cmRepoSyncOperationResult;

    private final CmParcelSyncOperationResult cmParcelSyncOperationResult;

    public CmSyncOperationResult(CmRepoSyncOperationResult cmRepoSyncOperationResult, CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        this.cmParcelSyncOperationResult = cmParcelSyncOperationResult;
        this.cmRepoSyncOperationResult = cmRepoSyncOperationResult;
    }

    public CmParcelSyncOperationResult getCmParcelSyncOperationResult() {
        return cmParcelSyncOperationResult;
    }

    public CmRepoSyncOperationResult getCmRepoSyncOperationResult() {
        return cmRepoSyncOperationResult;
    }

    public boolean isEmpty() {
        return cmRepoSyncOperationResult.isEmpty() && cmParcelSyncOperationResult.isEmpty();
    }

    @Override
    public String toString() {
        return "CmSyncOperationResult{" +
                "cmRepoSyncOperationResult=" + cmRepoSyncOperationResult +
                ", cmParcelSyncOperationResult=" + cmParcelSyncOperationResult +
                '}';
    }

}
