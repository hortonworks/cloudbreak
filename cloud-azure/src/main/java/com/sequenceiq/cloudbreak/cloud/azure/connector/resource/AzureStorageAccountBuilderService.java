package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AzureStorageAccountBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageAccountBuilderService.class);

    private static final Pattern POLICY_DEFINITION_NAME_PATTERN = Pattern.compile("\"policyDefinitionDisplayName\":\"(.*?)\"");

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
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Storage account creation");
        } catch (Exception e) {
            String message = String.format("Could not create storage account %s in resource group %s%s",
                    storageAccountName, resourceGroupName, getDeniedPolicyReasonIfApplicable(e));
            LOGGER.warn(message, e);
            throw new CloudConnectorException(message);
        }
    }

    /**
     * Workaround for
     * <a href="https://github.com/Azure/autorest-clientruntime-for-java/commit/fcd2f69ac7c04ac50337b0c37fab7ac24d4cce09">azure-sdk bug</a>
     */
    private String getDeniedPolicyReasonIfApplicable(Exception e) {
        if (e.getMessage().equals("Unknown error with status code 400") && e.getCause() instanceof MismatchedInputException) {
            MismatchedInputException cause = (MismatchedInputException) e.getCause();
            Matcher matcher = POLICY_DEFINITION_NAME_PATTERN.matcher(cause.getLocation().sourceDescription());
            if (matcher.find()) {
                String policyDefinitionDisplayName = matcher.group(1);
                return String.format(", because it was denied by policy '%s'", policyDefinitionDisplayName);
            }
        }
        return "";
    }
}
