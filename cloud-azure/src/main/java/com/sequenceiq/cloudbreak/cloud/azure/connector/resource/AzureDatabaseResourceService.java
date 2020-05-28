package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureDatabaseResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseResourceService.class);

    // PostgreSQL server port is fixed for now
    private static final int POSTGRESQL_SERVER_PORT = 5432;

    private static final String DATABASE_SERVER_FQDN = "databaseServerFQDN";

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    public List<CloudResourceStatus> buildDatabaseResourcesForLaunch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier persistenceNotifier) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);

        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        Boolean useSingleResourceGroup = azureResourceGroupMetadataProvider.useSingleResourceGroup(stack);
        String template = azureTemplateBuilder.build(cloudContext, stack);

        if (!client.resourceGroupExists(resourceGroupName)) {
            if (useSingleResourceGroup) {
                LOGGER.warn("Resource group with {} does not exist", resourceGroupName);
                throw new CloudConnectorException(String.format("Resource group with %s does not exist!", resourceGroupName));
            } else {
                LOGGER.debug("Resource group with {} does not exist, creating it now..", resourceGroupName);
                String region = ac.getCloudContext().getLocation().getRegion().value();
                client.createResourceGroup(resourceGroupName, region, stack.getTags());
            }
        }
        try {
            client.createTemplateDeployment(resourceGroupName, stackName, template, "");
        } catch (CloudException e) {
            if (e.body() != null && e.body().details() != null) {
                String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                throw new CloudConnectorException(String.format("Database stack provisioning failed, status code %s, error message: %s, details: %s",
                        e.body().code(), e.body().message(), details), e);
            } else {
                throw new CloudConnectorException(String.format("Database stack provisioning failed: '%s', please go to Azure Portal for details",
                        e.getMessage()), e);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Error in provisioning database stack %s: %s", stackName, e.getMessage()), e);
        }

        Deployment deployment = client.getTemplateDeployment(resourceGroupName, stackName);
        String fqdn = (String) ((Map) ((Map) deployment.outputs()).get(DATABASE_SERVER_FQDN)).get("value");

        List<CloudResource> databaseResources = createCloudResources(resourceGroupName, fqdn);
        databaseResources.forEach(dbr -> persistenceNotifier.notifyAllocation(dbr, cloudContext));

        List<CloudResource> cloudResources = azureCloudResourceService.getCloudResources(deployment);
        cloudResources.forEach(cloudResource -> persistenceNotifier.notifyAllocation(cloudResource, cloudContext));

        return databaseResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }

    private List<CloudResource> createCloudResources(String resourceGroupName, String fqdn) {
        List<CloudResource> databaseResources = Lists.newArrayList();
        databaseResources.add(CloudResource.builder()
                .type(ResourceType.RDS_HOSTNAME)
                .name(fqdn)
                .build());
        databaseResources.add(CloudResource.builder()
                .type(ResourceType.RDS_PORT)
                .name(Integer.toString(POSTGRESQL_SERVER_PORT))
                .build());
        databaseResources.add(CloudResource.builder()
                .type(AZURE_RESOURCE_GROUP)
                .name(resourceGroupName)
                .build());
        return databaseResources;
    }

    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext ac, DatabaseStack stack, List<CloudResource> resources, boolean force) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);

        return azureResourceGroupMetadataProvider.useSingleResourceGroup(stack)
                ? deleteDatabaseServer(resources, force, client)
                : deleteResourceGroup(stack, force, cloudContext, client);
    }

    private List<CloudResourceStatus> deleteResourceGroup(DatabaseStack stack, boolean force, CloudContext cloudContext, AzureClient client) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        LOGGER.debug("Deleting resource group {}", resourceGroupName);
        try {
            client.deleteResourceGroup(resourceGroupName);
        } catch (CloudException e) {
            errorHandling(force, e, "resource group", resourceGroupName);
        }

        return Lists.newArrayList(new CloudResourceStatus(CloudResource.builder()
                .type(AZURE_RESOURCE_GROUP)
                .name(resourceGroupName)
                .build(), ResourceStatus.DELETED));
    }

    private List<CloudResourceStatus> deleteDatabaseServer(List<CloudResource> resources, boolean force, AzureClient client) {
        LOGGER.debug("Deleting database server");
        Optional<CloudResource> dbServerResourceOptional = resources.stream().filter(r -> AZURE_DATABASE.equals(r.getType())).findFirst();
        if (dbServerResourceOptional.isEmpty()) {
            LOGGER.warn("Azure database id not found in database, deleting nothing");
            return List.of();
        }
        String databaseServerId = dbServerResourceOptional.get().getReference();
        try {
            azureUtils.deleteDatabaseServer(client, databaseServerId);
        } catch (CloudException e) {
            errorHandling(force, e, "Database server", databaseServerId);
        }

        return Lists.newArrayList(new CloudResourceStatus(CloudResource.builder()
                .type(AZURE_DATABASE)
                .name(databaseServerId)
                .build(), ResourceStatus.DELETED));
    }

    private void errorHandling(boolean force, CloudException e, String resourceType, String resourceId) {
        String errorMessage = null;
        if (e.body() != null) {
            String errorCode = e.body().code();
            if ("ResourceGroupNotFound".equals(errorCode)) {
                LOGGER.warn("{} {} does not exist, assuming that it has already been deleted", resourceType, resourceId);
                // leave errorMessage null => do not throw exception
            } else {
                String details = e.body().details() != null ? e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", ")) : "";
                errorMessage = String.format("%s %s deletion failed, status code %s, error message: %s, details: %s",
                        resourceType, resourceId, errorCode, e.body().message(), details);
            }
        } else {
            errorMessage = String.format("%s %s deletion failed: '%s', please go to Azure Portal for details",
                    resourceType, resourceId, e.getMessage());
        }

        if (errorMessage != null) {
            if (force) {
                LOGGER.warn(errorMessage);
                LOGGER.warn("{} {} deletion failed, continuing because termination is forced", resourceType, resourceId);
            } else {
                throw new CloudConnectorException(errorMessage, e);
            }
        }
    }

    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext ac, DatabaseStack stack) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        try {
            ResourceGroup resourceGroup = client.getResourceGroup(resourceGroupName);
            if (resourceGroup == null) {
                return ExternalDatabaseStatus.DELETED;
            }
            return ExternalDatabaseStatus.STARTED;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new CloudConnectorException(e);
        }
    }
}

