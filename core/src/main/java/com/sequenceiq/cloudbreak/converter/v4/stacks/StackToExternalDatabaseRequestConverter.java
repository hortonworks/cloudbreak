package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.util.NullUtil.allNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class StackToExternalDatabaseRequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToExternalDatabaseRequestConverter.class);

    public DatabaseRequest convert(Stack source) {
        Database database = source.getDatabase();
        if (database == null || allNull(database.getExternalDatabaseAvailabilityType(), database.getExternalDatabaseEngineVersion())) {
            return null;
        }
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(source.getExternalDatabaseCreationType());
        databaseRequest.setDatabaseEngineVersion(source.getExternalDatabaseEngineVersion());
        databaseRequest.setDatalakeDatabaseAvailabilityType(database.getDatalakeDatabaseAvailabilityType());
        return databaseRequest;
    }

}
