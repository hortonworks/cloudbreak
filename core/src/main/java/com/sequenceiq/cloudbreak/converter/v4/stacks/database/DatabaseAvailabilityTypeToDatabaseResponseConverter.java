package com.sequenceiq.cloudbreak.converter.v4.stacks.database;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class DatabaseAvailabilityTypeToDatabaseResponseConverter  extends AbstractConversionServiceAwareConverter<DatabaseAvailabilityType, DatabaseResponse> {

    @Override
    public DatabaseResponse convert(DatabaseAvailabilityType source) {
        DatabaseResponse response = new DatabaseResponse();
        response.setAvailabilityType(source);
        return response;
    }
}
