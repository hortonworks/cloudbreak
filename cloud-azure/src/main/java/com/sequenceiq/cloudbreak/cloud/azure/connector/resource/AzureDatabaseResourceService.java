package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.azure.resourcemanager.postgresql.models.ServerState.DISABLED;
import static com.azure.resourcemanager.postgresql.models.ServerState.DROPPING;
import static com.azure.resourcemanager.postgresql.models.ServerState.INACCESSIBLE;
import static com.azure.resourcemanager.postgresql.models.ServerState.READY;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.DB_VERSION;
import static com.sequenceiq.common.api.type.CommonResourceType.CANARY;
import static com.sequenceiq.common.api.type.ResourceType.ARM_TEMPLATE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE_CANARY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_ENDPOINT;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_HOSTNAME;
import static com.sequenceiq.common.api.type.ResourceType.RDS_HOSTNAME_CANARY;
import static com.sequenceiq.common.api.type.ResourceType.RDS_PORT;
import static com.sequenceiq.common.model.PrivateEndpointType.USE_PRIVATE_ENDPOINT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresql.models.StorageProfile;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerState;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Storage;
import com.azure.resourcemanager.resources.models.Deployment;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.ResourceGroupUsage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureFlexibleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureSingleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTransientDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureTemplateDeploymentFailureReasonProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureRDSAutoMigrationValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.service.CloudResourceValidationService;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;

@Service
public class AzureDatabaseResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseResourceService.class);

    // PostgreSQL server port is fixed for now
    private static final int POSTGRESQL_SERVER_PORT = 5432;

    private static final String DATABASE_SERVER_FQDN = "databaseServerFQDN";

    private static final Map<ServerState, ExternalDatabaseStatus> FLEXIBLESERVER_STATE_MAP = Map.of(
            ServerState.DISABLED, ExternalDatabaseStatus.DELETED,
            ServerState.READY, ExternalDatabaseStatus.STARTED,
            ServerState.DROPPING, ExternalDatabaseStatus.DELETE_IN_PROGRESS,
            ServerState.STOPPING, ExternalDatabaseStatus.STOP_IN_PROGRESS,
            ServerState.STOPPED, ExternalDatabaseStatus.STOPPED,
            ServerState.STARTING, ExternalDatabaseStatus.START_IN_PROGRESS,
            ServerState.UPDATING, ExternalDatabaseStatus.UPDATE_IN_PROGRESS,
            AzureFlexibleServerClient.UNKNOWN, ExternalDatabaseStatus.UNKNOWN);

    private static final Map<com.azure.resourcemanager.postgresql.models.ServerState, ExternalDatabaseStatus> SINGLESERVER_STATE_MAP = Map.of(
            DISABLED, ExternalDatabaseStatus.DELETED,
            READY, ExternalDatabaseStatus.STARTED,
            DROPPING, ExternalDatabaseStatus.DELETE_IN_PROGRESS,
            INACCESSIBLE, ExternalDatabaseStatus.UNKNOWN,
            AzureSingleServerClient.UNKNOWN, ExternalDatabaseStatus.UNKNOWN);

    private static final long GB_TO_MB = 1024L;

    @Inject
    private AzureDatabaseTemplateBuilder azureDatabaseTemplateBuilder;

    @Inject
    private AzureDatabaseTemplateProvider azureDatabaseTemplateProvider;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureTransientDeploymentService azureTransientDeploymentService;

    @Inject
    private AzurePermissionValidator azurePermissionValidator;

    @Inject
    private AzureRDSAutoMigrationValidator azureRDSAutoMigrationValidator;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private CloudResourceValidationService cloudResourceValidationService;

    @Inject
    private AzureTemplateDeploymentFailureReasonProvider azureTemplateDeploymentFailureReasonProvider;

    public List<CloudResourceStatus> buildDatabaseResourcesForLaunch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier persistenceNotifier) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);

        DatabaseServer databaseServer = stack.getDatabaseServer();
        azurePermissionValidator.validateFlexibleServerPermission(client, databaseServer);
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        ResourceGroupUsage resourceGroupUsage = azureResourceGroupMetadataProvider.getResourceGroupUsage(stack);
        String template = azureDatabaseTemplateBuilder.build(cloudContext, stack);

        createResourceGroupIfNotExists(ac, stack, client, resourceGroupName, resourceGroupUsage, persistenceNotifier);

        createTemplateResource(persistenceNotifier, cloudContext, stackName);
        List<CloudResource> cloudResources = createOrFetchDeployment(persistenceNotifier, stackName, resourceGroupName, template, client, ac);

        AzureDatabaseType databaseType = getAzureDatabaseType(stack);
        if (AzureDatabaseType.FLEXIBLE_SERVER.equals(databaseType)) {
            setUpPublicAccessBasedOnNetworkSettings(stack, client, databaseServer, resourceGroupName);
            addAzureExtensionsToFlexibleServerWithRetry(client, resourceGroupName, databaseServer.getServerId());
        }

        return convertWithStatus(cloudResources);
    }

    private void setUpPublicAccessBasedOnNetworkSettings(DatabaseStack stack, AzureClient client, DatabaseServer databaseServer, String resourceGroupName) {
        AzureNetworkView azureNetworkView = new AzureNetworkView(stack.getNetwork());
        boolean useDelegatedSubnet = StringUtils.hasText(azureNetworkView.getExistingDatabasePrivateDnsZoneId()) &&
                StringUtils.hasText(azureNetworkView.getFlexibleServerDelegatedSubnetId());
        boolean usePrivateEndpoints = USE_PRIVATE_ENDPOINT.equals(azureNetworkView.getEndpointType());
        LOGGER.info("Checking if firewall rule creation necessary: useDelegatedSubnet: {}, usePrivateEndpoints: {}", useDelegatedSubnet, usePrivateEndpoints);
        if (!useDelegatedSubnet && !usePrivateEndpoints) {
            try {
                createPublicAccessFirewallRuleForFlexibleDbWithRetry(client, databaseServer, resourceGroupName);
            } catch (Retry.ActionFailedException e) {
                throw azureUtils.convertToCloudConnectorException(e.getCause(), "Firewall rule creation failed with conflict after several attempts.");
            } catch (ManagementException e) {
                throw azureUtils.convertToCloudConnectorException(e, "Firewall rule creation failed.");
            }
        }
    }

    private void createPublicAccessFirewallRuleForFlexibleDbWithRetry(AzureClient client, DatabaseServer databaseServer, String resourceGroupName) {
        LOGGER.info("Creating firewall rule for public access.");
        retryService.testWith2SecDelayMax5Times(() -> {
            try {
                client.createPublicAccessFirewallRuleForFlexibleDb(databaseServer.getServerId(), resourceGroupName);
            } catch (ManagementException e) {
                if (azureExceptionHandler.isExceptionCodeConflict(e)) {
                    LOGGER.info("Firewall rule for public access failed with a conflict. It's retried 5 times before failing.", e);
                    throw Retry.ActionFailedException.ofCause(e);
                } else {
                    throw e;
                }
            }
        });
    }

    private void addAzureExtensionsToFlexibleServerWithRetry(AzureClient client, String resourceGroupName, String serverName) {
        try {
            retryService.testWith2SecDelayMax5Times(() -> {
                try {
                    client.addAzureExtensionsToFlexibleServer(resourceGroupName, serverName);
                } catch (ManagementException e) {
                    if (azureExceptionHandler.isExceptionCodeConflict(e)) {
                        LOGGER.info("Adding azure.extensions failed. It's retried 5 times before failing.", e);
                        throw Retry.ActionFailedException.ofCause(e);
                    } else {
                        throw e;
                    }
                }
            });
        } catch (Retry.ActionFailedException e) {
            LOGGER.warn("Adding Azure extensions failed after several attempts.", e);
        } catch (ManagementException e) {
            LOGGER.warn("Adding Azure extensions failed.", e);
        }
    }

    private List<CloudResourceStatus> convertWithStatus(List<CloudResource> cloudResources) {
        return cloudResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }

    private List<CloudResource> createOrFetchDeployment(PersistenceNotifier persistenceNotifier, String stackName, String resourceGroupName, String template,
            AzureClient client, AuthenticatedContext ac) {
        Optional<RuntimeException> exception = Optional.empty();
        try {
            deployDatabaseServer(stackName, resourceGroupName, template, client, ac);
        } catch (ManagementException e) {
            exception = Optional.ofNullable(azureUtils.convertToCloudConnectorException(e, "Database stack provisioning"));
        } catch (CloudConnectorException cce) {
            exception = Optional.of(cce);
        } catch (Exception e) {
            exception = Optional.of(new CloudConnectorException(String.format("Error in provisioning database stack %s: %s", stackName, e.getMessage()), e));
        }
        List<CloudResource> resources = fetchAndSaveDeploymentResources(persistenceNotifier, stackName, resourceGroupName, client, ac, exception);
        LOGGER.debug("Deployment resources: {}", resources);
        return resources;
    }

    private List<CloudResource> fetchAndSaveDeploymentResources(PersistenceNotifier persistenceNotifier, String stackName, String resourceGroupName,
            AzureClient client, AuthenticatedContext ac, Optional<RuntimeException> exception) {
        try {
            Deployment deployment = fetchDeploymentWithRetry(stackName, resourceGroupName, client);
            List<CloudResource> cloudResources = new ArrayList<>();
            boolean canaryDeployment = false;
            if (Objects.nonNull(deployment)) {
                cloudResources = azureCloudResourceService.getDeploymentCloudResources(deployment);
                canaryDeployment = isCanaryDeployment(cloudResources);

                String fqdn = (String) ((Map) ((Map) deployment.outputs()).get(DATABASE_SERVER_FQDN)).get("value");
                List<CloudResource> databaseResources = createCloudResources(fqdn, canaryDeployment);

                cloudResources.addAll(databaseResources);
                cloudResources.forEach(dbr -> persistenceNotifier.notifyAllocation(dbr, ac.getCloudContext()));
            } else {
                LOGGER.info("Deployment with name {} in RG {} was not found, this should not happen", stackName, resourceGroupName);
            }
            if (exception.isPresent()) {
                if (canaryDeployment) {
                    LOGGER.warn("Canary deployment failed, cleaning up resources {}", cloudResources);
                    deleteCanaryDatabaseForUpgrade(ac, persistenceNotifier, cloudResources);
                }
                throw exception.get();
            } else {
                return cloudResources;
            }
        } catch (Retry.ActionFailedException e) {
            LOGGER.warn("Error during fetching database deployment", e);
            Deployment deployment = fetchDeployment(stackName, resourceGroupName, client);
            List<CloudResource> cloudResources = azureCloudResourceService.getDeploymentCloudResources(deployment);
            boolean canaryDeployment = isCanaryDeployment(cloudResources);
            if (canaryDeployment) {
                LOGGER.warn("Canary deployment failed and template retry exhausted, cleaning up resources {}", cloudResources);
                deleteCanaryDatabaseForUpgrade(ac, persistenceNotifier, cloudResources);
            }
            throw exception.orElse(e);
        }
    }

    private boolean isCanaryDeployment(List<CloudResource> cloudResources) {
        return cloudResources.stream().allMatch(resource -> resource.getCommonResourceType() == CANARY);
    }

    private Deployment fetchDeploymentWithRetry(String stackName, String resourceGroupName, AzureClient client) {
        return retryService.testWith2SecDelayMax5Times(() -> {
            Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
            if (templateDeployment == null || templateDeployment.outputs() == null) {
                LOGGER.warn("Template deployment or it's output not found: {}", templateDeployment);
                throw new Retry.ActionFailedException("Deployment or it's output not found");
            } else {
                return templateDeployment;
            }
        });
    }

    private Deployment fetchDeployment(String stackName, String resourceGroupName, AzureClient client) {
        Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
        LOGGER.warn("Template deployment: {}", templateDeployment);
        return templateDeployment;
    }

    private void createResourceGroupIfNotExists(AuthenticatedContext ac, DatabaseStack stack, AzureClient client, String resourceGroupName,
            ResourceGroupUsage resourceGroupUsage, PersistenceNotifier persistenceNotifier) {
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
        createResourceGroupResource(persistenceNotifier, ac.getCloudContext(), resourceGroupName);
    }

    private void createTemplateResource(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, String stackName) {
        CloudResource armTemplate = createCloudResource(ARM_TEMPLATE, stackName);
        persistenceNotifier.notifyAllocation(armTemplate, cloudContext);
    }

    private void createResourceGroupResource(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, String resourceGroupName) {
        CloudResource resourceGroup = createCloudResource(AZURE_RESOURCE_GROUP, resourceGroupName);
        persistenceNotifier.notifyAllocation(resourceGroup, cloudContext);
    }

    private List<CloudResource> createCloudResources(String fqdn, boolean canaryDeployment) {
        return List.of(
                createCloudResource(canaryDeployment ? RDS_HOSTNAME_CANARY : RDS_HOSTNAME, fqdn),
                createCloudResource(ResourceType.RDS_PORT, Integer.toString(POSTGRESQL_SERVER_PORT)));
    }

    private CloudResource createCloudResource(ResourceType type, String name) {
        return CloudResource.builder()
                .withType(type)
                .withName(name)
                .build();
    }

    public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(stack.getDatabaseServer());
        if (azureDatabaseServerView.getAzureDatabaseType() == AzureDatabaseType.FLEXIBLE_SERVER) {
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(authenticatedContext.getCloudContext(), stack);
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            LOGGER.debug("Starting flexible database server {} in resourcegroup {}", stack.getDatabaseServer().getServerId(), resourceGroupName);
            client.getFlexibleServerClient().startFlexibleServer(resourceGroupName, stack.getDatabaseServer().getServerId());
        } else {
            LOGGER.debug("Start database server is not supported for {} database type", azureDatabaseServerView.getAzureDatabaseType());
        }
    }

    public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(stack.getDatabaseServer());
        if (azureDatabaseServerView.getAzureDatabaseType() == AzureDatabaseType.FLEXIBLE_SERVER) {
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(authenticatedContext.getCloudContext(), stack);
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            LOGGER.debug("Stopping flexible database server {} in resourcegroup {}", stack.getDatabaseServer().getServerId(), resourceGroupName);
            client.getFlexibleServerClient().stopFlexibleServer(resourceGroupName, stack.getDatabaseServer().getServerId());
        } else {
            LOGGER.debug("Stop database server is not supported for {} database type", azureDatabaseServerView.getAzureDatabaseType());
        }
    }

    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext ac, DatabaseStack stack,
            List<CloudResource> resources, boolean force, PersistenceNotifier persistenceNotifier) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);
        return (azureResourceGroupMetadataProvider.getResourceGroupUsage(stack) != ResourceGroupUsage.MULTIPLE)
                ? deleteResources(resources, cloudContext, force, client, persistenceNotifier, stack.getDatabaseServer())
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
            AzureClient client, PersistenceNotifier persistenceNotifier, DatabaseServer databaseServer) {

        // TODO simplify after final form of template is reached

        List<CloudResource> azureGenericResources = findResources(resources, List.of(AZURE_PRIVATE_ENDPOINT));
        LOGGER.debug("Deleting Azure private endpoints {}", azureGenericResources);
        azureUtils.deleteGenericResources(client, azureGenericResources.stream().map(CloudResource::getReference).collect(Collectors.toList()));
        azureGenericResources.forEach(cr -> persistenceNotifier.notifyDeletion(cr, cloudContext));
        return findResources(resources, List.of(AZURE_DATABASE))
                .stream()
                .map(r -> deleteDatabaseServerAndNotify(r, cloudContext, client, persistenceNotifier, force, databaseServer))
                .collect(Collectors.toList());
    }

    private CloudResourceStatus deleteDatabaseServerAndNotify(CloudResource cloudResource, CloudContext cloudContext, AzureClient client,
            PersistenceNotifier persistenceNotifier, boolean force, DatabaseServer databaseServer) {
        LOGGER.debug("Deleting postgres server {}", cloudResource.getReference());
        deleteKeyVault(client, cloudResource, databaseServer);
        azureUtils.deleteDatabaseServer(client, cloudResource.getReference(), force);
        persistenceNotifier.notifyDeletion(cloudResource, cloudContext);
        return new CloudResourceStatus(CloudResource.builder()
                .withType(AZURE_DATABASE)
                .withName(cloudResource.getReference())
                .build(), ResourceStatus.DELETED);
    }

    private void deleteKeyVault(AzureClient client, CloudResource cloudResource, DatabaseServer databaseServer) {
        if (databaseServer != null) {
            AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(databaseServer);
            String keyVaultUrl = azureDatabaseServerView.getKeyVaultUrl();
            String keyVaultResourceGroupName = azureDatabaseServerView.getKeyVaultResourceGroupName();
            AzureDatabaseType azureDatabaseType = azureDatabaseServerView.getAzureDatabaseType();
            if (azureDatabaseType == AzureDatabaseType.SINGLE_SERVER && keyVaultUrl != null && keyVaultResourceGroupName != null) {
                String dbPrincipalId = client.getServicePrincipalForResourceById(cloudResource.getReference());
                String vaultName = client.getVaultNameFromEncryptionKeyUrl(keyVaultUrl);
                if (vaultName != null) {
                    // Check for the existence of keyVault user has specified before removing Database access permissions from this keyVault.
                    if (!client.keyVaultExists(keyVaultResourceGroupName, vaultName)) {
                        LOGGER.warn(String.format(
                                "Vault with name \"%s\" either does not exist/has been deleted or user does not have permissions to access it.", vaultName));
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
        }
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
            return getExternalDatabaseStatus(stack.getDatabaseServer(), client, resourceGroupName);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    private ExternalDatabaseStatus getExternalDatabaseStatus(DatabaseServer databaseServer, AzureClient client, String resourceGroupName) {
        return getExternalDatabaseParameters(databaseServer, client, resourceGroupName).externalDatabaseStatus();
    }

    public ExternalDatabaseParameters getExternalDatabaseParameters(AuthenticatedContext ac, DatabaseStack stack) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        return getExternalDatabaseParameters(stack.getDatabaseServer(), client, resourceGroupName);
    }

    private ExternalDatabaseParameters getExternalDatabaseParameters(DatabaseServer databaseServer, AzureClient client, String resourceGroupName) {
        AzureDatabaseServerView databaseServerView = new AzureDatabaseServerView(databaseServer);
        ExternalDatabaseParameters externalDatabaseParameters;
        if (databaseServerView.getAzureDatabaseType() == AzureDatabaseType.FLEXIBLE_SERVER) {
            LOGGER.debug("Getting flexible server parameters from Azure for {} database", databaseServerView.getDbServerName());
            Server server = client.getFlexibleServerClient().getFlexibleServer(resourceGroupName, databaseServer.getServerId());
            externalDatabaseParameters = new ExternalDatabaseParameters(
                    convertFlexibleStatus(server),
                    AzureDatabaseType.FLEXIBLE_SERVER,
                    getFlexibleServerStorageSizeInMB(server));
        } else {
            LOGGER.debug("Getting single server parameters from Azure for {} database", databaseServerView.getDbServerName());
            com.azure.resourcemanager.postgresql.models.Server server =
                    client.getSingleServerClient().getSingleServer(resourceGroupName, databaseServer.getServerId());
            externalDatabaseParameters = new ExternalDatabaseParameters(
                    convertSingleStatus(server),
                    AzureDatabaseType.SINGLE_SERVER,
                    getSingleServerStorageSizeInMB(server));
        }
        LOGGER.debug("External database parameters: {}", externalDatabaseParameters);
        return externalDatabaseParameters;
    }

    private ExternalDatabaseStatus convertFlexibleStatus(Server server) {
        return Optional.ofNullable(server)
                .map(Server::state)
                .map(state -> FLEXIBLESERVER_STATE_MAP.getOrDefault(state, ExternalDatabaseStatus.UNKNOWN))
                .orElse(ExternalDatabaseStatus.DELETED);
    }

    private ExternalDatabaseStatus convertSingleStatus(com.azure.resourcemanager.postgresql.models.Server server) {
        return Optional.ofNullable(server)
                .map(com.azure.resourcemanager.postgresql.models.Server::userVisibleState)
                .map(state -> SINGLESERVER_STATE_MAP.getOrDefault(state, ExternalDatabaseStatus.UNKNOWN))
                .orElse(ExternalDatabaseStatus.DELETED);
    }

    private Long getSingleServerStorageSizeInMB(com.azure.resourcemanager.postgresql.models.Server server) {
        return Optional.ofNullable(server)
                .map(com.azure.resourcemanager.postgresql.models.Server::storageProfile)
                .map(StorageProfile::storageMB)
                .map(Integer::longValue)
                .orElse(null);
    }

    private Long getFlexibleServerStorageSizeInMB(Server server) {
        return Optional.ofNullable(server)
                .map(Server::storage)
                .map(Storage::storageSizeGB)
                .map(size -> size * GB_TO_MB)
                .orElse(null);
    }

    public String getDBStackTemplate(DatabaseStack databaseStack) {
        return azureDatabaseTemplateProvider.getDBTemplateString(databaseStack);
    }

    public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack dbStack) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        azurePermissionValidator.validateFlexibleServerPermission(client);
        azureRDSAutoMigrationValidator.validate(authenticatedContext, dbStack);
    }

    public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack originalStack, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) {

        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        if (getAzureDatabaseType(originalStack) == AzureDatabaseType.FLEXIBLE_SERVER) {
            upgradeFlexibleServer(authenticatedContext, targetMajorVersion, resources, resourceGroupName);
        } else {
            upgradeSingleServer(authenticatedContext, stack, persistenceNotifier, targetMajorVersion, resources, cloudContext,
                    resourceGroupName);
        }
    }

    private void upgradeSingleServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, PersistenceNotifier persistenceNotifier,
            TargetMajorVersion targetMajorVersion, List<CloudResource> resources, CloudContext cloudContext, String resourceGroupName) {
        LOGGER.debug("Upgrading Single Server to version {}", targetMajorVersion.getMajorVersion());
        String stackName = azureUtils.getStackName(cloudContext);
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        try {
            deleteDatabaseResourcesOnProvider(persistenceNotifier, resources, cloudContext, client, false);

            DatabaseServer databaseServer = stack.getDatabaseServer();
            databaseServer.putParameter(DB_VERSION, targetMajorVersion.getMajorVersion());
            String template = azureDatabaseTemplateBuilder.build(cloudContext, stack);
            deployDatabaseServer(stackName, resourceGroupName, template, client, authenticatedContext);
            setUpPublicAccessBasedOnNetworkSettings(stack, client, databaseServer, resourceGroupName);
            addAzureExtensionsToFlexibleServerWithRetry(client, resourceGroupName, databaseServer.getServerId());
        } catch (ManagementException e) {
            Optional<String> deploymentOperationError = azureTemplateDeploymentFailureReasonProvider.getFailureMessage(resourceGroupName, stackName, client);
            throw azureUtils.convertToCloudConnectorExceptionWithFailureReason(e, "Database stack upgrade", deploymentOperationError.orElse(null));
        } catch (CloudConnectorException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Error occurred in upgrading database stack %s: %s", stackName, e.getMessage()), e);
        } finally {
            recreateCloudResourcesInDeployment(persistenceNotifier, cloudContext, stackName, resourceGroupName, client);
        }
    }

    public void deleteDatabaseResourcesOnProvider(PersistenceNotifier persistenceNotifier, List<CloudResource> resources, CloudContext cloudContext,
            AzureClient client, boolean canary) {
        deleteAllPrivateEndpointResources(persistenceNotifier, cloudContext, resources, client, canary);
        deleteDatabaseServerResources(persistenceNotifier, cloudContext, resources, client, canary);
    }

    private void upgradeFlexibleServer(AuthenticatedContext authenticatedContext, TargetMajorVersion targetMajorVersion, List<CloudResource> resources,
            String resourceGroupName) {
        try {
            LOGGER.debug("Upgrading Flexible Server to version {}", targetMajorVersion.getMajorVersion());
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            Optional<CloudResource> databaseServer = getResources(resources, AZURE_DATABASE).stream().findFirst();
            databaseServer.ifPresentOrElse(
                    databaseServerResource -> client.getFlexibleServerClient().upgrade(
                            resourceGroupName,
                            databaseServerResource.getName(),
                            targetMajorVersion.getMajorVersion()),
                    () -> {
                        String message = "Azure database server cloud resource does not exist for stack, this should not happen. " +
                                "Please contact Cloudera support to get this resolved.";
                        LOGGER.warn(message);
                        throw new CloudConnectorException(message);
                    });
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Database stack upgrade");
        } catch (CloudConnectorException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Error occurred in upgrading database stack: %s", e.getMessage()), e);
        }
    }

    public void updateAdministratorLoginPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        String serverName = databaseStack.getDatabaseServer().getServerId();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(authenticatedContext.getCloudContext(), databaseStack);
        try {
            AzureDatabaseType azureDatabaseType = getAzureDatabaseType(databaseStack);
            LOGGER.info("Update default admin user password for database: {}, database type: {}", serverName, azureDatabaseType);
            if (azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER) {
                client.getFlexibleServerClient().updateAdministratorLoginPassword(resourceGroupName, serverName, newPassword);
            } else {
                client.getSingleServerClient().updateAdministratorLoginPassword(resourceGroupName, serverName, newPassword);
            }
            LOGGER.info("Default admin user password updated for database: {}", serverName);
        } catch (Exception e) {
            LOGGER.warn("Update default admin user password failed for database: {}, reason: {}", serverName, e.getMessage());
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private AzureDatabaseType getAzureDatabaseType(DatabaseStack databaseStack) {
        return AzureDatabaseType.safeValueOf(databaseStack.getDatabaseServer().getStringParameter(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY));
    }

    private void deleteDatabaseServerResources(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, List<CloudResource> resources,
            AzureClient client, boolean canary) {
        ResourceType azureDatabase = canary ? AZURE_DATABASE_CANARY : AZURE_DATABASE;
        Optional<CloudResource> databaseServer = getResources(resources, azureDatabase, false)
                .stream()
                .findFirst();
        databaseServer.ifPresentOrElse(
                databaseServerResource -> deleteDatabaseServerResources(client, databaseServerResource, persistenceNotifier, cloudContext),
                () -> {
                    String message = "Azure database server cloud resource does not exist for stack, ignoring it now";
                    LOGGER.warn(message);
                });
        List<CloudResource> rdsDescriptorResources = canary ?
                findResources(resources, List.of(RDS_HOSTNAME_CANARY, RDS_PORT)) :
                findResources(resources, List.of(RDS_HOSTNAME, RDS_PORT));
        deleteResources(rdsDescriptorResources, cloudContext, persistenceNotifier);
    }

    private void deleteAllPrivateEndpointResources(PersistenceNotifier persistenceNotifier, CloudContext cloudContext, List<CloudResource> resources,
            AzureClient client, boolean canary) {
        azureCloudResourceService.getPrivateEndpointRdsResourceTypes(canary)
                .stream()
                .map(resourceType -> getResources(resources, resourceType, false))
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
        return getResources(resources, resourceType, true);
    }

    private List<CloudResource> getResources(List<CloudResource> resources, ResourceType resourceType, boolean createdOnly) {
        return resources.stream()
                .filter(resource -> resource.getType() == resourceType)
                .filter(resource -> !createdOnly || resource.getStatus() == CommonStatus.CREATED)
                .collect(Collectors.toList());
    }

    private void deployDatabaseServer(String stackName, String resourceGroupName, String template, AzureClient client,
            AuthenticatedContext ac) {
        if (client.getTemplateDeploymentStatus(resourceGroupName, stackName).isPermanent()) {
            LOGGER.debug("Re-deploying database server {} in resource group {}", stackName, resourceGroupName);
            String parametersMapAsString = new Json(Map.of()).getValue();
            try {
                createTemplateDeploymentWithRetryInCaseOfConflict(stackName, resourceGroupName, template, client, parametersMapAsString);
            } catch (Retry.ActionFailedException e) {
                throw (ManagementException) e.getCause();
            }
        } else {
            waitForDeployment(stackName, resourceGroupName, ac);
        }
    }

    private void waitForDeployment(String stackName, String resourceGroupName, AuthenticatedContext ac) {
        try {
            LOGGER.debug("The database server template deployment is in progress with name {}, let's wait for it...", stackName);
            CloudResource templateResource = createCloudResource(ARM_TEMPLATE, resourceGroupName);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, List.of(templateResource), true);
            ResourcesStatePollerResult statePollerResult = syncPollingScheduler.schedule(task);
            cloudResourceValidationService.validateResourcesState(ac.getCloudContext(), statePollerResult);
            LOGGER.debug("The database server template deployment is done with name {}", stackName);
        } catch (CloudConnectorException cce) {
            LOGGER.debug("Cloud connector exception during waiting for template deployment with name {}", stackName, cce);
            throw cce;
        } catch (Exception ex) {
            LOGGER.debug("Exception during waiting for template deployment with name {}", stackName, ex);
            throw new CloudConnectorException(ex.getMessage(), ex);
        }
    }

    private void createTemplateDeploymentWithRetryInCaseOfConflict(String stackName, String resourceGroupName, String template, AzureClient client,
            String parametersMapAsString) {
        retryService.testWith2SecDelayMax5Times(() -> {
            try {
                client.createTemplateDeployment(resourceGroupName, stackName, template, parametersMapAsString);
            } catch (ManagementException e) {
                if (azureExceptionHandler.isExceptionCodeConflict(e)) {
                    LOGGER.info("Database server deployment failed with a conflict. It's retried 5 times before failing.", e);
                    throw Retry.ActionFailedException.ofCause(e);
                } else {
                    throw e;
                }
            }
        });
    }

    private void deleteDatabaseServerResources(AzureClient client, CloudResource resource, PersistenceNotifier persistenceNotifier, CloudContext cloudContext) {
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

    public List<CloudResourceStatus> launchCanaryDatabaseForUpgrade(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            DatabaseStack migratedDbStack, PersistenceNotifier persistenceNotifier) {
        List<CloudResource> resources = new ArrayList<>();
        AzureDatabaseServerView azureDatabaseServer = new AzureDatabaseServerView(stack.getDatabaseServer());
        boolean originalDbSingleServer = azureDatabaseServer.getAzureDatabaseType().isSingleServer();
        if (originalDbSingleServer) {
            CloudContext cloudContext = authenticatedContext.getCloudContext();
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, migratedDbStack);
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            DatabaseServer databaseServer = migratedDbStack.getDatabaseServer();
            ExternalDatabaseStatus externalDatabaseStatus = getExternalDatabaseStatus(databaseServer, client, resourceGroupName);
            String deploymentName = databaseServer.getServerId();
            if (externalDatabaseStatus.isRelaunchable()) {
                String template = azureDatabaseTemplateBuilder.build(cloudContext, migratedDbStack);
                resources = createOrFetchDeployment(persistenceNotifier, deploymentName, resourceGroupName, template, client, authenticatedContext);
                setUpPublicAccessBasedOnNetworkSettings(stack, client, databaseServer, resourceGroupName);
                addAzureExtensionsToFlexibleServerWithRetry(client, resourceGroupName, databaseServer.getServerId());
            } else {
                LOGGER.debug("Database server deployment is already present with status {} and name {}, so skipping canary launch",
                        externalDatabaseStatus, deploymentName);
            }
        } else {
            LOGGER.debug("Database is not Single Server ({}), no need to run canary deployment!", azureDatabaseServer.getAzureDatabaseType());
        }
        return convertWithStatus(resources);
    }

    public void deleteCanaryDatabaseForUpgrade(AuthenticatedContext authenticatedContext, PersistenceNotifier persistenceNotifier,
            List<CloudResource> resources) {
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        deleteDatabaseResourcesOnProvider(persistenceNotifier, resources, cloudContext, client, true);
    }
}