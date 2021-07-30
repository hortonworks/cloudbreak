package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

public class CmRepoSyncOperationResult {

    private final String installedCmVersion;

    private final ClouderaManagerRepo foundClouderaManagerRepo;

    public CmRepoSyncOperationResult(String installedCmVersion, ClouderaManagerRepo foundClouderaManagerRepo) {
        this.installedCmVersion = installedCmVersion;
        this.foundClouderaManagerRepo = foundClouderaManagerRepo;
    }

    public Optional<String> getInstalledCmVersion() {
        return Optional.ofNullable(installedCmVersion);
    }

    public Optional<ClouderaManagerRepo> getFoundClouderaManagerRepo() {
        return Optional.ofNullable(foundClouderaManagerRepo);
    }

}
