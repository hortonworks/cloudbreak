package com.sequenceiq.mock.spi;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;

public class DbDto {

    private final DatabaseStack databaseStack;

    private ExternalDatabaseStatus externalDatabaseStatus;

    private final String mockuuid;

    public DbDto(String mockuuid, DatabaseStack databaseStack, ExternalDatabaseStatus externalDatabaseStatus) {
        this.databaseStack = databaseStack;
        this.mockuuid = mockuuid;
        this.externalDatabaseStatus = externalDatabaseStatus;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }

    public ExternalDatabaseStatus getExternalDatabaseStatus() {
        return externalDatabaseStatus;
    }

    public void setExternalDatabaseStatus(ExternalDatabaseStatus externalDatabaseStatus) {
        this.externalDatabaseStatus = externalDatabaseStatus;
    }
}
