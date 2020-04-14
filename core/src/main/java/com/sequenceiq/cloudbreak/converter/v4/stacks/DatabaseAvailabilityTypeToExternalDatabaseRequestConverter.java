package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class DatabaseAvailabilityTypeToExternalDatabaseRequestConverter
        extends AbstractConversionServiceAwareConverter<DatabaseAvailabilityType, DatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAvailabilityTypeToExternalDatabaseRequestConverter.class);

    @Override
    public DatabaseRequest convert(DatabaseAvailabilityType source) {
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(source);
        return databaseRequest;
    }
}
