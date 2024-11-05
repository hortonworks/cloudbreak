package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.io.Serializable;

import com.sequenceiq.common.model.AzureDatabaseType;

public record AzureRDSAutoMigrationParams(
        AzureDatabaseType azureDatabaseType,
        String serverId)
        implements Serializable {
}
