package com.sequenceiq.distrox.v1.distrox.converter;

import static java.lang.String.format;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;

@Component
public class DistroXDatabaseRequestToStackDatabaseRequestConverter {

    private static final String UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT = "Unexpected database availability type: %s";

    public DatabaseRequest convert(DistroXDatabaseRequest source) {
        DatabaseRequest request = new DatabaseRequest();
        request.setAvailabilityType(convertAvailabilityType(source.getAvailabilityType()));
        return request;
    }

    public DistroXDatabaseRequest convert(DatabaseRequest source) {
        DistroXDatabaseRequest request = new DistroXDatabaseRequest();
        request.setAvailabilityType(convertAvailabilityType(source.getAvailabilityType()));
        return request;
    }

    private DatabaseAvailabilityType convertAvailabilityType(DistroXDatabaseAvailabilityType availabilityType) {
        if (availabilityType == null) {
            throw new IllegalArgumentException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, null));
        }
        switch (availabilityType) {
            case NONE:
                return DatabaseAvailabilityType.NONE;
            case NON_HA:
                return DatabaseAvailabilityType.NON_HA;
            case HA:
                return DatabaseAvailabilityType.HA;
            default:
                throw new IllegalStateException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, availabilityType.name()));
        }
    }

    private DistroXDatabaseAvailabilityType convertAvailabilityType(DatabaseAvailabilityType availabilityType) {
        if (availabilityType == null) {
            throw new IllegalArgumentException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, null));
        }
        switch (availabilityType) {
            case NONE:
                return DistroXDatabaseAvailabilityType.NONE;
            case NON_HA:
                return DistroXDatabaseAvailabilityType.NON_HA;
            case HA:
                return DistroXDatabaseAvailabilityType.HA;
            default:
                throw new IllegalStateException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, availabilityType.name()));
        }
    }

}
