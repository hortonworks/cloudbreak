package com.sequenceiq.datalake.service.upgrade.database;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.DatabaseBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;

@Service
public class DatabaseEngineVersionReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEngineVersionReaderService.class);

    @Inject
    private DatabaseService databaseService;

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    public Optional<MajorVersion> getDatabaseServerMajorVersion(SdxCluster sdxCluster) {
        Optional<MajorVersion> databaseServerMajorVersion = sdxCluster.hasExternalDatabase()
                ? getExternalDbMajorVersion(sdxCluster)
                : getEmbeddedDatabaseVersion(sdxCluster);
        LOGGER.debug("Database server major version is now {} for SDX {}", databaseServerMajorVersion, sdxCluster);
        return databaseServerMajorVersion;
    }

    private Optional<MajorVersion> getExternalDbMajorVersion(SdxCluster cluster) {
        StackDatabaseServerResponse stackDatabaseServerResponse = databaseService.getDatabaseServer(cluster.getDatabaseCrn());
        Optional<MajorVersion> currentVersionFromRedbeams = Optional.ofNullable(stackDatabaseServerResponse.getMajorVersion());
        LOGGER.debug("Queried external database server major version from redbeams for external DB: {}", currentVersionFromRedbeams);
        return currentVersionFromRedbeams;
    }

    private Optional<MajorVersion> getEmbeddedDatabaseVersion(SdxCluster sdxCluster) {
        StackV4Response stackV4Response = cloudbreakStackService.getStack(sdxCluster);
        Optional<MajorVersion> currentMajorVersionInStackOptional = MajorVersion.get(
                Optional.ofNullable(stackV4Response.getExternalDatabase())
                        .map(DatabaseBase::getDatabaseEngineVersion).orElse(null));
        LOGGER.debug("Queried embedded database server major version from stack, found {}", currentMajorVersionInStackOptional);
        return currentMajorVersionInStackOptional;
    }

}