package com.sequenceiq.datalake.service.upgrade.database;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;

@Service
public class DatabaseEngineVersionReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEngineVersionReaderService.class);

    @Inject
    private DatabaseService databaseService;

    public Optional<MajorVersion> getDatabaseServerMajorVersion(SdxCluster sdxCluster) {
        StackDatabaseServerResponse stackDatabaseServerResponse = databaseService.getDatabaseServer(sdxCluster.getDatabaseCrn());
        Optional<MajorVersion> databaseServerMajorVersion = Optional.ofNullable(stackDatabaseServerResponse.getMajorVersion());
        LOGGER.debug("Database server major version is now {} for SDX {}", databaseServerMajorVersion, sdxCluster);
        return databaseServerMajorVersion;
    }
}
