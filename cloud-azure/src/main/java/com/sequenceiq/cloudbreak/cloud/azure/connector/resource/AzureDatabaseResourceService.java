package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.DB_VERSION;
import static com.sequenceiq.common.api.type.ResourceType.ARM_TEMPLATE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_ENDPOINT;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_HOSTNAME;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.ResourceGroupUsage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTransientDeploymentService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureDatabaseResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseResourceService.class);

    // PostgreSQL server port is fixed for now
    private static final int POSTGRESQL_SERVER_PORT = 5432;

    private static final String DATABASE_SERVER_FQDN = "databaseServerFQDN";

    @Inject
    private AzureDatabaseTemplateBuilder azureDatabaseTemplateBuilder;

    @Inject
    private AzureDatabaseTemplateProvider azureDatabaseTemplateProvider;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureTransientDeploymentService azureTransientDeploymentService;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    public List<CloudResourceStatus> buildDatabaseResourcesForLaunch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier persistenceNotifier) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);

        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        ResourceGroupUsage resourceGroupUsage = azureResourceGroupMetadataProvider.getResourceGroupUsage(stack);
        String template = azureDatabaseTemplateBuilder.build(cloudContext, stack);

        if (!client.resourceGroupExists(resourceGroupName)) {
            if (resourceGroupUsage != ResourceGroupUsage.MULTIPLE) {
                LOGGER.warn("Resource group with name {} does not exist", resourceGroupName);
                throw new CloudConnectorException(String.format("Resource group with name %s does not exist!", resourceGroupName));
            } else {
                LOGGER.debug("Resource group with name {} does not exist, creating it now..", resourceGroupName);
                String region = ac.getCloudContext().getLocation().getRegion().value();
                client.createResourceGroup(resourceGroupName, region, stack.getTags());
            }
        }
        createResourceGroupResource(persistenceNotifier, cloudContext, resourceGroupName);
        createTemplateResource(persistenceNotifier, cloudContext, stackName);
        Deployment deployment;
        try {
            deployDatabaseServer(stackName, resourceGroupName, template, client);
        } catch (CloudException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Database stack provisioning");
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Error in provisioning database stack %s: %s", stackName, e.getMessage()), e);
        } finally {
            deployment = client.getTemplateDeployment(resourceGroupName, stackName);
            if (deployment != null) {
                List<CloudResource> cloudResources = azureCloudResourceService.getDeploymentCloudResources(deployment);
                cloudResources.forEach(cloudResource -> persistenceNotifier.notifyAllocation(cloudResource, cloudContext));
            }
        }

        String fqdn = (String) ((Map) ((Map) deployment.outputs()).get(DATABASE_SERVER_FQDN)).get("value");
        List<CloudResource> databaseResources = createCloudResources(fqdn);
        databaseResources.forEach(dbr -> persistenceNotifier.notifyAllocation(dbr, cloudContext));
        return databaseResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }

    private void createTemplateResource(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, String stackName) {
        CloudResource armTemplate = createCloudResource(ARM_TEMPLATE, stackName);
        persistenceNotifier.notifyAllocation(armTemplate, cloudContext);
    }

    private void createResourceGroupResource(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, String resourceGroupName) {
        CloudResource resourceGroup = createCloudResource(AZURE_RESOURCE_GROUP, resourceGroupName);
        persistenceNotifier.notifyAllocation(resourceGroup, cloudContext);
    }

    private List<CloudResource> createCloudResources(String fqdn) {
        List<CloudResource> databaseResources = Lists.newArrayList();
        databaseResources.add(createCloudResource(RDS_HOSTNAME, fqdn));
        databaseResources.add(createCloudResource(ResourceType.RDS_PORT, Integer.toString(POSTGRESQL_SERVER_PORT)));
        return databaseResources;
    }

    private CloudResource createCloudResource(ResourceType type, String name) {
        return CloudResource.builder()
                .withType(type)
                .withName(name)
                .build();
    }

    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext ac, DatabaseStack stack,
            List<CloudResource> resources, boolean force, PersistenceNotifier persistenceNotifier) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);
        String keyVaultUrl = null;
        String keyVaultResourceGroupName = null;
        Map<String, Object> dbStackParams = Optional.ofNullable(stack.getDatabaseServer())
                .map(DatabaseServer::getParameters).orElse(null);
        if (dbStackParams != null && dbStackParams.containsKey("keyVaultUrl")) {
            keyVaultUrl = (String) dbStackParams.get("keyVaultUrl");
            keyVaultResourceGroupName = (String) dbStackParams.getOrDefault("keyVaultResourceGroupName", null);
        }
        return (azureResourceGroupMetadataProvider.getResourceGroupUsage(stack) != ResourceGroupUsage.MULTIPLE)
                ? deleteResources(resources, cloudContext, force, client, persistenceNotifier, keyVaultUrl, keyVaultResourceGroupName)
                : deleteResourceGroup(resources, cloudContext, force, client, persistenceNotifier, stack);
    }

    public void handleTransientDeployment(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        Optional<String> deploymentNameOpt = getFirstResourceName(resources, ARM_TEMPLATE);
        Optional<String> resourceGroupNameOpt = getFirstResourceName(resources, AZURE_RESOURCE_GROUP);
        LOGGER.debug("Database template saved: {}, resource group saved: {}", deploymentNameOpt, resourceGroupNameOpt);
        if (deploymentNameOpt.isPresent() && resourceGroupNameOpt.isPresent()) {
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            String resourceGroupName = resourceGroupNameOpt.get();
            String deploymentName = deploymentNameOpt.get();
            LOGGER.debug("Checking if database deployment {}.{} status is transient", resourceGroupName, deploymentName);
            resources.addAll(azureTransientDeploymentService.handleTransientDeployment(client, resourceGroupName, deploymentName));
        }
    }

    private Optional<String> getFirstResourceName(List<CloudResource> resources, ResourceType resourceType) {
        return findResources(resources, List.of(resourceType)).stream()
                .map(CloudResource::getName)
                .findFirst();
    }

    private List<CloudResourceStatus> deleteResourceGroup(List<CloudResource> resources, CloudContext cloudContext, boolean force,
            AzureClient client, PersistenceNotifier persistenceNotifier, DatabaseStack stack) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        Optional<String> errorMessage = azureUtils.deleteResourceGroup(client, resourceGroupName, force);
        if (errorMessage.isEmpty()) {
            deleteResources(resources, cloudContext, persistenceNotifier);
        }
        return Lists.newArrayList(new CloudResourceStatus(CloudResource.builder()
                .withType(AZURE_RESOURCE_GROUP)
                .withName(resourceGroupName)
                .build(), ResourceStatus.DELETED));
    }

    private List<CloudResourceStatus> deleteResources(List<CloudResource> resources, CloudContext cloudContext, boolean force,
            AzureClient client, PersistenceNotifier persistenceNotifier, String keyVaultUrl, String keyVaultResourceGroupName) {

        // TODO simplify after final form of template is reached

        List<CloudResource> azureGenericResources = findResources(resources, List.of(AZURE_PRIVATE_ENDPOINT));
        LOGGER.debug("Deleting Azure private endpoints {}", azureGenericResources);
        azureUtils.deleteGenericResources(client, azureGenericResources.stream().map(CloudResource::getReference).collect(Collectors.toList()));
        azureGenericResources.forEach(cr -> persistenceNotifier.notifyDeletion(cr, cloudContext));

        return findResources(resources, List.of(AZURE_DATABASE)).stream()
                .map(r -> deleteDatabaseServerAndNotify(r, cloudContext, client, persistenceNotifier, force, keyVaultUrl, keyVaultResourceGroupName))
                .collect(Collectors.toList());
    }

    private CloudResourceStatus deleteDatabaseServerAndNotify(
            CloudResource cloudResource, CloudContext cloudContext, AzureClient client, PersistenceNotifier persistenceNotifier, boolean force,
            String keyVaultUrl, String keyVaultResourceGroupName) {
        LOGGER.debug("Deleting postgres server {}", cloudResource.getReference());
        if (keyVaultUrl != null && keyVaultResourceGroupName != null) {
            String dbPrincipalId = client.getServicePrincipalForResourceById(cloudResource.getReference());
            String vaultName = client.getVaultNameFromEncryptionKeyUrl(keyVaultUrl);
            if (vaultName != null) {
                // Check for the existence of keyVault user has specified before removing Database access permissions from this keyVault.
                if (!client.keyVaultExists(keyVaultResourceGroupName, vaultName)) {
                    LOGGER.warn(String.format("Vault with name \"%s\" either does not exist/has been deleted or user does not have permissions to access it.",
                            vaultName));
                } else {
                    String description = String.format("access to Key Vault \"%s\" in Resource Group \"%s\" for Service Principal having object ID \"%s\" " +
                                    "associated with Database.", vaultName, keyVaultResourceGroupName, dbPrincipalId);
                    retryService.testWith2SecDelayMax15Times(() -> {
                        try {
                            LOGGER.info("Removing {}.", description);
                            client.removeKeyVaultAccessPolicyForServicePrincipal(keyVaultResourceGroupName, vaultName, dbPrincipalId);
                            LOGGER.info("Removed {}.", description);
                            return true;
                        } catch (Exception e) {
                            throw azureUtils.convertToActionFailedExceptionCausedByCloudConnectorException(e, "Removing " + description);
                        }
                    });
                }
            } else {
                LOGGER.warn("vaultName cannot be fetched from encryptionKeyUrl - {}. Access policy for the database cannot be removed " +
                        "from the vault.", keyVaultUrl);
            }
        } else {
            LOGGER.info("Database is not encrypted with CMK.");
        }
        azureUtils.deleteDatabaseServer(client, cloudResource.getReference(), force);
        persistenceNotifier.notifyDeletion(cloudResource, cloudContext);
        return new CloudResourceStatus(CloudResource.builder()
                .withType(AZURE_DATABASE)
                .withName(cloudResource.getReference())
                .build(), ResourceStatus.DELETED);
    }

    private List<CloudResource> findResources(List<CloudResource> resources, List<ResourceType> resourceTypes) {
        return resources.stream().filter(r -> resourceTypes.contains(r.getType())).collect(Collectors.toList());
    }

    private void deleteResources(List<CloudResource> cloudResourceList, CloudContext cloudContext, PersistenceNotifier persistenceNotifier) {
        cloudResourceList.forEach(r -> {
            LOGGER.debug("Deleting resource {} from db", r);
            persistenceNotifier.notifyDeletion(r, cloudContext);
        });
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

    public String getDBStackTemplate() {
        return azureDatabaseTemplateProvider.getDBTemplateString();
    }

    public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) {

        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);

        try {
            deleteDatabaseServer(persistenceNotifier, cloudContext, resources, client);
            deleteAllPrivateEndpointResources(persistenceNotifier, cloudContext, resources, client);

            stack.getDatabaseServer().putParameter(DB_VERSION, targetMajorVersion.getMajorVersion());
            String template = azureDatabaseTemplateBuilder.build(cloudContext, stack);
            deployDatabaseServer(stackName, resourceGroupName, template, client);
        } catch (CloudException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Database stack upgrade");
        } catch (CloudConnectorException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Error in upgrading database stack %s: %s", stackName, e.getMessage()), e);
        } finally {
            recreateCloudResourcesInDeployment(persistenceNotifier, cloudContext, stackName, resourceGroupName, client);
        }
    }

    private void deleteDatabaseServer(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, List<CloudResource> resources, AzureClient client) {
        Optional<CloudResource> databaseServer = getResources(resources, AZURE_DATABASE).stream().findFirst();
        databaseServer.ifPresentOrElse(
                databaseServerResource -> deleteDatabaseServer(client, databaseServerResource, persistenceNotifier, cloudContext),
                () -> {
                    String message = "Azure database server cloud resource does not exist for stack, please contact Cloudera support!";
                    LOGGER.warn(message);
                    throw new CloudConnectorException(message);
                });
    }

    private void deleteAllPrivateEndpointResources(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, List<CloudResource> resources,
            AzureClient client) {
        azureCloudResourceService.getPrivateEndpointRdsResourceTypes()
                .stream()
                .map(resourceType -> getResources(resources, resourceType))
                .forEach(filteredResources -> filteredResources.forEach(
                        resource -> deleteResource(client, resource, persistenceNotifier, cloudContext)));
    }

    private void recreateCloudResourcesInDeployment(PersistenceNotifier persistenceNotifier, CloudContext cloudContext,
            String deploymentName, String resourceGroupName, AzureClient client) {
        Deployment deployment = client.getTemplateDeployment(resourceGroupName, deploymentName);
        if (deployment != null) {
            List<CloudResource> cloudResources = azureCloudResourceService.getDeploymentCloudResources(deployment);
            LOGGER.debug("Deployment {} has been found with the following cloud resources: {}", deploymentName, cloudResources);
            persistenceNotifier.notifyAllocations(cloudResources, cloudContext);
        } else {
            LOGGER.warn("Deployment {} is not found, it should not happen", deploymentName);
        }
    }

    private List<CloudResource> getResources(List<CloudResource> resources, ResourceType resourceType) {
        return resources.stream()
                .filter(resource -> resource.getType() == resourceType)
                .filter(resource -> resource.getStatus() == CommonStatus.CREATED)
                .collect(Collectors.toList());
    }

    private void deployDatabaseServer(String stackName, String resourceGroupName, String template, AzureClient client) {
        LOGGER.debug("Re-deploying database server {} in resource group {}", stackName, resourceGroupName);
        String parametersMapAsString = new Json(Map.of()).getValue();
        client.createTemplateDeployment(resourceGroupName, stackName, template, parametersMapAsString);
    }

    private void deleteDatabaseServer(AzureClient client, CloudResource resource, PersistenceNotifier persistenceNotifier, CloudContext cloudContext) {
        String databaseReference = resource.getReference();
        LOGGER.debug("Azure database server has been found with the reference '{}', deleting and marking it 'DETACHED' in our database: {}",
                databaseReference, resource);
        azureUtils.deleteDatabaseServer(client, databaseReference, false);
        persistenceNotifier.notifyDeletion(resource, cloudContext);
    }

    private void deleteResource(AzureClient client, CloudResource resource, PersistenceNotifier persistenceNotifier, CloudContext cloudContext) {
        ResourceType resourceType = resource.getType();
        LOGGER.debug("Deleting {} from our database: {}", resourceType, resource);
        azureUtils.deleteGenericResourceById(client, resource.getReference(), AzureResourceType.getByResourceType(resourceType));
        persistenceNotifier.notifyDeletion(resource, cloudContext);
    }
}
