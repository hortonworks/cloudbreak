package com.sequenceiq.distrox.v1.distrox.converter;

import static java.lang.String.format;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAzureRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;

@Component
public class DistroXDatabaseRequestToStackDatabaseRequestConverter {

    private static final String UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT = "Unexpected database availability type: %s";

    public DatabaseRequest convert(DistroXDatabaseRequest source) {
        DatabaseRequest request = new DatabaseRequest();
        request.setAvailabilityType(convertAvailabilityType(source.getAvailabilityType()));
        request.setDatabaseEngineVersion(source.getDatabaseEngineVersion());
        request.setDatabaseAzureRequest(convertDistroxDatabaseAzureRequest(source.getDatabaseAzureRequest()));
        return request;
    }

    public DistroXDatabaseRequest convert(DatabaseRequest source) {
        DistroXDatabaseRequest request = new DistroXDatabaseRequest();
        if (source.getAvailabilityType() != null) {
            request.setAvailabilityType(convertAvailabilityType(source.getAvailabilityType()));
        }
        request.setDatabaseEngineVersion(source.getDatabaseEngineVersion());
        request.setDatabaseAzureRequest(convertDatabaseAzureRequest(source.getDatabaseAzureRequest()));
        return request;
    }

    private DatabaseAvailabilityType convertAvailabilityType(DistroXDatabaseAvailabilityType availabilityType) {
        return switch (availabilityType) {
            case NONE -> DatabaseAvailabilityType.NONE;
            case NON_HA -> DatabaseAvailabilityType.NON_HA;
            case HA -> DatabaseAvailabilityType.HA;
            case ON_ROOT_VOLUME -> DatabaseAvailabilityType.ON_ROOT_VOLUME;
            default -> throw new IllegalStateException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, availabilityType.name()));
        };
    }

    private DistroXDatabaseAvailabilityType convertAvailabilityType(DatabaseAvailabilityType availabilityType) {
        if (availabilityType == null) {
            throw new IllegalArgumentException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, "null"));
        }
        return switch (availabilityType) {
            case NONE -> DistroXDatabaseAvailabilityType.NONE;
            case NON_HA -> DistroXDatabaseAvailabilityType.NON_HA;
            case HA -> DistroXDatabaseAvailabilityType.HA;
            case ON_ROOT_VOLUME -> DistroXDatabaseAvailabilityType.ON_ROOT_VOLUME;
            default -> throw new IllegalStateException(format(UNEXPECTED_AVAILABILITY_TYPE_MSG_FORMAT, availabilityType.name()));
        };
    }

    private DatabaseAzureRequest convertDistroxDatabaseAzureRequest(DistroXDatabaseAzureRequest distroxDatabaseAzureRequest) {
        if (distroxDatabaseAzureRequest != null) {
            DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
            databaseAzureRequest.setAzureDatabaseType(distroxDatabaseAzureRequest.getAzureDatabaseType());
            databaseAzureRequest.setFlexibleServerDelegatedSubnetId(distroxDatabaseAzureRequest.getFlexibleServerDelegatedSubnetId());
            return databaseAzureRequest;
        } else {
            return null;
        }
    }

    private DistroXDatabaseAzureRequest convertDatabaseAzureRequest(DatabaseAzureRequest databaseAzureRequest) {
        if (databaseAzureRequest != null) {
            DistroXDatabaseAzureRequest distroXDatabaseAzureRequest = new DistroXDatabaseAzureRequest();
            distroXDatabaseAzureRequest.setAzureDatabaseType(databaseAzureRequest.getAzureDatabaseType());
            distroXDatabaseAzureRequest.setFlexibleServerDelegatedSubnetId(databaseAzureRequest.getFlexibleServerDelegatedSubnetId());
            return distroXDatabaseAzureRequest;
        } else {
            return null;
        }
    }

}
