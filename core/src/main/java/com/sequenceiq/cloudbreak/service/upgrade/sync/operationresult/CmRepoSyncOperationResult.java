package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

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

    public boolean isEmpty() {
        return StringUtils.isEmpty(installedCmVersion);
    }

    @Override
    public String toString() {
        return "CmRepoSyncOperationResult{" +
                "installedCmVersion='" + installedCmVersion + '\'' +
                ", foundClouderaManagerRepo=" + foundClouderaManagerRepo +
                '}';
    }

}
