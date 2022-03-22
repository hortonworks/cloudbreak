package com.sequenceiq.cloudbreak.converter.v4.stacks.database;

import static com.sequenceiq.cloudbreak.util.NullUtil.allNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;

@Component
public class ExternalDatabaseToDatabaseResponseConverter {

    public DatabaseResponse convert(DatabaseAvailabilityType availabilityType, String externalDatabaseEngineVersion) {
        if (allNull(availabilityType, externalDatabaseEngineVersion)) {
            return null;
        } else {
            DatabaseResponse response = new DatabaseResponse();
            response.setAvailabilityType(availabilityType);
            response.setDatabaseEngineVersion(externalDatabaseEngineVersion);
            return response;
        }
    }
}
