package com.sequenceiq.redbeams.service.validation;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.service.EnvironmentService;

@Component
public class DatabaseEncryptionValidator {

    @Inject
    private EnvironmentService environmentService;

    public void validateEncryption(String cloudPlatform, DatabaseServer databaseServer) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            AzureDatabaseServerView azureDatabaseServer = new AzureDatabaseServerView(databaseServer);
            if (!azureDatabaseServer.getAzureDatabaseType().isSingleServer() && StringUtils.isNotEmpty(azureDatabaseServer.getKeyVaultUrl()) &&
                    StringUtils.isEmpty(azureDatabaseServer.getEncryptionUserManagedIdentity())) {
                throw new BadRequestException(
                        "Database server provisioning validation failed because Azure Flexible server requires dedicated maneged identity " +
                                "for data encryption. Please add the managed identity via environment edit and try to recreate your cluster. " +
                                "You could find more info at " + DocumentationLinkProvider.azureFlexibleServerCMKManagedIdentityLink());
            }
        }
    }

    public void validateEncryptionDuringUpgrade(String cloudPlatform, String environmentCrn, DatabaseServer databaseServer, TargetMajorVersion targetVersion) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            AzureDatabaseServerView azureDatabaseServer = new AzureDatabaseServerView(databaseServer);
            boolean targetVersion14 = TargetMajorVersion.VERSION14.getMajorVersion().equals(targetVersion.getMajorVersion());
            boolean singleServer = azureDatabaseServer.getAzureDatabaseType().isSingleServer();
            if (singleServer && targetVersion14 &&
                    StringUtils.isNotEmpty(azureDatabaseServer.getKeyVaultUrl()) && !environmentContainsEncryptionParameters(environmentCrn)) {
                throw new BadRequestException(
                        "Database server upgrade validation failed because Azure Flexible server requires dedicated managed identity for data encryption. "
                                + "Please edit your environment, and retry the upgrade. "
                                + DocumentationLinkProvider.azureFlexibleServerCMKManagedIdentityLink());
            }
        }
    }

    private boolean environmentContainsEncryptionParameters(String environmentCrn) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(environmentCrn);
        AzureResourceEncryptionParameters resourceEncryptionParameters = environment.getAzure().getResourceEncryptionParameters();
        return resourceEncryptionParameters != null && StringUtils.isNotEmpty(resourceEncryptionParameters.getUserManagedIdentity());
    }
}
