package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AzureStorageAccountBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageAccountBuilderService.class);

    @Inject
    private AzureStorageAccountTemplateBuilder azureStorageAccountTemplateBuilder;

    public void buildStorageAccount(AzureClient client, StorageAccountParameters storageAccountParameters) {
        String resourceGroupName = storageAccountParameters.getResourceGroupName();
        String storageAccountName = storageAccountParameters.getStorageAccountName();
        try {
            String template = azureStorageAccountTemplateBuilder.build(storageAccountParameters);
            if (!client.templateDeploymentExists(resourceGroupName, storageAccountName)) {
                String parameters = new Json(Map.of()).getValue();
                Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, storageAccountName, template, parameters);
                LOGGER.debug("Created template deployment for storage account: {}", templateDeployment.exportTemplate().template());
            }
        } catch (CloudException e) {
            LOGGER.info("Provisioning error, cloud exception happened: ", e);
            if (e.body() != null && e.body().details() != null) {
                String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                throw new CloudConnectorException(String.format("Storage account provisioning failed, status code %s, error message: %s, details: %s",
                        e.body().code(), e.body().message(), details));
            } else {
                throw new CloudConnectorException(String.format("Storage account provisioning failed: '%s', please go to Azure Portal for detailed message", e));
            }
        } catch (Exception e) {
            String message = String.format("Could not create storage account %s in resource group %s", storageAccountName, resourceGroupName);
            LOGGER.warn(message, e);
            throw new CloudConnectorException(message);
        }
    }
}
