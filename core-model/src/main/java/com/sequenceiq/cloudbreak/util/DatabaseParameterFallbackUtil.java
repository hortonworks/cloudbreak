package com.sequenceiq.cloudbreak.util;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.view.StackView;

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
        database.setExternalDatabaseAvailabilityType(databaseAvailabilityType);
        database.setExternalDatabaseEngineVersion(dbEngineVersion);
        database.setAttributes(attributes);
        // TODO only for backward compatibility, can be removed in CB-22002
        stack.setExternalDatabaseCreationType(databaseAvailabilityType);
        stack.setExternalDatabaseEngineVersion(dbEngineVersion);
        return database;
    }

    public static String getExternalDatabaseEngineVersion(Database database, String fallbackExternalDatabaseEngineVersion) {
        String result;
        if (database != null) {
            String dbEngineVersion = Optional.of(database).map(Database::getExternalDatabaseEngineVersion).orElse("");
            if (fallbackExternalDatabaseEngineVersion != null && !dbEngineVersion.equals(fallbackExternalDatabaseEngineVersion)) {
                result = fallbackExternalDatabaseEngineVersion;
            } else {
                result = dbEngineVersion;
            }
        } else {
            result = fallbackExternalDatabaseEngineVersion;
        }
        return result;
    }

    public static DatabaseAvailabilityType getExternalDatabaseCreationType(Database database, DatabaseAvailabilityType fallbackExternalDatabaseCreationType) {
        return database != null ? database.getExternalDatabaseAvailabilityType() : fallbackExternalDatabaseCreationType;
    }

    public static Database getOrCreateDatabase(Stack stack) {
        if (stack.getDatabase() == null) {
            Database database = new Database();
            database.setExternalDatabaseAvailabilityType(stack.getExternalDatabaseCreationType());
            database.setExternalDatabaseEngineVersion(stack.getExternalDatabaseEngineVersion());
            return database;
        } else {
            if (!stack.getDatabase().getExternalDatabaseEngineVersion().equals(stack.getExternalDatabaseEngineVersion())) {
                Database database = new Database();
                database.setExternalDatabaseAvailabilityType(stack.getExternalDatabaseCreationType());
                database.setExternalDatabaseEngineVersion(stack.getExternalDatabaseEngineVersion());
                return database;
            } else {
                return stack.getDatabase();
            }
        }
    }

    public static Database getOrCreateDatabase(Database database, StackView stack) {
        if (database == null) {
            Database result = new Database();
            result.setExternalDatabaseEngineVersion(stack.getExternalDatabaseEngineVersion());
            result.setExternalDatabaseAvailabilityType(stack.getExternalDatabaseCreationType());
            return result;
        } else {
            if (!stack.getExternalDatabaseEngineVersion().equals(database.getExternalDatabaseEngineVersion())) {
                database.setExternalDatabaseEngineVersion(stack.getExternalDatabaseEngineVersion());
            }
            return database;
        }
    }
}
