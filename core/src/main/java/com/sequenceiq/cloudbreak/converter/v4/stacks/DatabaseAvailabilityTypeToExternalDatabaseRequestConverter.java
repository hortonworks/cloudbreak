package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;

@Component
public class DatabaseAvailabilityTypeToExternalDatabaseRequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAvailabilityTypeToExternalDatabaseRequestConverter.class);

    public DatabaseRequest convert(DatabaseAvailabilityType source) {
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(source);
        return databaseRequest;
    }
}
