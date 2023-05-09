package com.sequenceiq.cloudbreak.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

// TODO It's only needed for handling backward compatibility, can be removed in CB-22002
public class DatabaseParameterFallbackUtil {
    private DatabaseParameterFallbackUtil() {
    }

    public static Database setupDatabaseInitParams(Stack stack, DatabaseAvailabilityType externalDatabaseCreationType, String externalDatabaseEngineVersion) {
        return setupDatabaseInitParams(stack, externalDatabaseCreationType, externalDatabaseEngineVersion, null);
    }

    public static Database setupDatabaseInitParams(Stack stack, DatabaseAvailabilityType databaseAvailabilityType, String dbEngineVersion,
            Json attributes) {
        Database database = new Database();
        database.setExternalDatabaseCreationType(databaseAvailabilityType);
        database.setExternalDatabaseEngineVersion(dbEngineVersion);
        database.setAttributes(attributes);
        // TODO only for backward compatibility, can be removed in CB-22002
        stack.setExternalDatabaseCreationType(databaseAvailabilityType);
        stack.setExternalDatabaseEngineVersion(dbEngineVersion);
        return database;
    }

    public static String getExternalDatabaseEngineVersion(Database database, String fallbackExternalDatabaseEngineVersion) {
        return database != null ? database.getExternalDatabaseEngineVersion() : fallbackExternalDatabaseEngineVersion;
    }

    public static DatabaseAvailabilityType getExternalDatabaseCreationType(Database database, DatabaseAvailabilityType fallbackExternalDatabaseCreationType) {
        return database != null ? database.getExternalDatabaseCreationType() : fallbackExternalDatabaseCreationType;
    }
}
