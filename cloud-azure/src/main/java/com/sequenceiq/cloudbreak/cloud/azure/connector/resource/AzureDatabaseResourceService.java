package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
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
    private DefaultCostTaggingService defaultCostTaggingService;

    public List<CloudResourceStatus> buildDatabaseResourcesForLaunch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier persistenceNotifier) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);

        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureUtils.getResourceGroupName(cloudContext, stack);
        String template = azureTemplateBuilder.build(cloudContext, stack);
        String region = ac.getCloudContext().getLocation().getRegion().value();

        try {
            if (!client.resourceGroupExists(resourceGroupName)) {
                client.createResourceGroup(resourceGroupName, region, stack.getTags(), defaultCostTaggingService.prepareTemplateTagging());
            }
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
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
                .type(ResourceType.AZURE_RESOURCE_GROUP)
                .name(resourceGroupName)
                .build());
        return databaseResources;
    }

    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext ac, DatabaseStack stack, boolean force) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = azureUtils.getResourceGroupName(cloudContext, stack);

        try {
            client.deleteResourceGroup(resourceGroupName);
        } catch (CloudException e) {
            String errorMessage = null;
            if (e.body() != null) {
                String errorCode = e.body().code();
                if ("ResourceGroupNotFound".equals(errorCode)) {
                    LOGGER.warn("Resource group {} does not exist, assuming that it has already been deleted", resourceGroupName);
                    // leave errorMessage null => do not throw exception
                } else {
                    String details = e.body().details() != null ? e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", ")) : "";
                    errorMessage = String.format("Resource group %s deletion failed, status code %s, error message: %s, details: %s",
                            resourceGroupName, errorCode, e.body().message(), details);
                }
            } else {
                errorMessage = String.format("Resource group %s deletion failed: '%s', please go to Azure Portal for details",
                        resourceGroupName, e.getMessage());
            }

            if (errorMessage != null) {
                if (force) {
                    LOGGER.warn(errorMessage);
                    LOGGER.warn("Resource group {} deletion failed, continuing because termination is forced", resourceGroupName);
                } else {
                    throw new CloudConnectorException(errorMessage, e);
                }
            }
        }

        return Lists.newArrayList(new CloudResourceStatus(CloudResource.builder()
                .type(ResourceType.AZURE_RESOURCE_GROUP)
                .name(resourceGroupName)
                .build(), ResourceStatus.DELETED));
    }
}

