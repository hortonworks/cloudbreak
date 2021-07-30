package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Optional;

public class CmSyncOperationResult {

    private final CmRepoSyncOperationResult cmRepoSyncOperationResult;

    private final CmParcelSyncOperationResult cmParcelSyncOperationResult;

    public CmSyncOperationResult(CmRepoSyncOperationResult cmRepoSyncOperationResult, CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        this.cmParcelSyncOperationResult = cmParcelSyncOperationResult;
        this.cmRepoSyncOperationResult = cmRepoSyncOperationResult;
    }

    public Optional<CmParcelSyncOperationResult> getCmParcelSyncOperationResult() {
        return Optional.ofNullable(cmParcelSyncOperationResult);
    }

    public Optional<CmRepoSyncOperationResult> getCmRepoSyncOperationResult() {
        return Optional.ofNullable(cmRepoSyncOperationResult);
    }

    public boolean isEmpty() {
        return cmRepoSyncOperationResult == null && cmParcelSyncOperationResult == null;
    }
}
