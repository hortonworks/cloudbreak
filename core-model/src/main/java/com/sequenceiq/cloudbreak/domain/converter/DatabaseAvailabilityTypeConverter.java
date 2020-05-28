package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class DatabaseAvailabilityTypeConverter extends DefaultEnumConverter<DatabaseAvailabilityType> {

    @Override
    public DatabaseAvailabilityType getDefault() {
        return DatabaseAvailabilityType.NONE;
    }
}
