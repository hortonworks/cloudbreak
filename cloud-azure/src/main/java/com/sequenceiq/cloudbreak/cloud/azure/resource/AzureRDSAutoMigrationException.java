package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.exception.RdsAutoMigrationException;

public class AzureRDSAutoMigrationException extends RdsAutoMigrationException {
    private final AzureRDSAutoMigrationParams azureRDSAutoMigrationParams;

    public AzureRDSAutoMigrationException(String message, AzureRDSAutoMigrationParams azureRDSAutoMigrationParams) {
        super(message);
        this.azureRDSAutoMigrationParams = azureRDSAutoMigrationParams;
    }

    public Optional<AzureRDSAutoMigrationParams> getAzureRDSAutoMigrationParams() {
        return Optional.ofNullable(azureRDSAutoMigrationParams);
    }
}
