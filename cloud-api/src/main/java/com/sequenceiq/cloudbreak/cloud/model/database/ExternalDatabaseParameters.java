package com.sequenceiq.cloudbreak.cloud.model.database;

import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.common.model.DatabaseType;

public record ExternalDatabaseParameters(
        ExternalDatabaseStatus externalDatabaseStatus,
        DatabaseType databaseType,
        Long storageSizeInMB) {
}
