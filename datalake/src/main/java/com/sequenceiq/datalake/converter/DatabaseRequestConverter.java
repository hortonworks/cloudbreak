package com.sequenceiq.datalake.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.datalake.entity.SdxCluster;

@Component
public class DatabaseRequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRequestConverter.class);

    public DatabaseRequest createExternalDbRequest(SdxCluster sdxCluster) {
        DatabaseRequest request = new DatabaseRequest();
        request.setAvailabilityType(DatabaseAvailabilityType.NONE);
        request.setDatabaseEngineVersion(sdxCluster.getDatabaseEngineVersion());
        LOGGER.debug("Created DB request: {}", request);
        return request;
    }
}
