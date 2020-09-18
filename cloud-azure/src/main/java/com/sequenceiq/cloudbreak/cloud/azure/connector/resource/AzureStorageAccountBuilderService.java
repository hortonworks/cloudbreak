package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.storage.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AzureStorageAccountBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageAccountBuilderService.class);

    @Inject
    private AzureStorageAccountTemplateBuilder azureStorageAccountTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    public StorageAccount buildStorageAccount(AzureClient client, StorageAccountParameters storageAccountParameters) {
        String resourceGroupName = storageAccountParameters.getResourceGroupName();
        String storageAccountName = storageAccountParameters.getStorageAccountName();
        try {
            String template = azureStorageAccountTemplateBuilder.build(storageAccountParameters);
            ResourceStatus templateDeploymentStatus = client.getTemplateDeploymentStatus(resourceGroupName, storageAccountName);
            LOGGER.debug("Template deployment status retrieved: {}.", templateDeploymentStatus);
            if (templateDeploymentStatus.isPermanent()) {
                LOGGER.debug("Creating template deployment.");
                String parameters = new Json(Map.of()).getValue();
                Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, storageAccountName, template, parameters);
                LOGGER.debug("Created template deployment for storage account: {}", templateDeployment.exportTemplate().template());
            }
            return client.getStorageAccountByGroup(resourceGroupName, storageAccountName);
        } catch (CloudException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Storage account creation");
        } catch (Exception e) {
            String message = String.format("Could not create storage account %s in resource group %s", storageAccountName, resourceGroupName);
            LOGGER.warn(message, e);
            throw new CloudConnectorException(message);
        }
    }
}
