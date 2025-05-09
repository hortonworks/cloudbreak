package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDatabaseResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.upscale.AzureUpscaleService;
import com.sequenceiq.cloudbreak.cloud.azure.upscale.AzureVerticalScaleService;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureResourceConnector extends AbstractResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceConnector.class);

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureComputeResourceService azureComputeResourceService;

    @Inject
    private AzureDatabaseResourceService azureDatabaseResourceService;

    @Inject
    private AzureUpscaleService azureUpscaleService;

    @Inject
    private AzureVerticalScaleService azureVerticalScaleService;

    @Inject
    private AzureStackViewProvider azureStackViewProvider;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureTerminationHelperService azureTerminationHelperService;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    @Inject
    private List<ProviderResourceSyncer> providerResourceSyncers;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        AzureClient client = ac.getParameter(AzureClient.class);
        AzureStackView azureStackView = azureStackViewProvider.getAzureStack(azureCredentialView, stack, client, ac);
        String template;

        Image stackImage = stack.getImage();
        if (azureImageFormatValidator.isMarketplaceImageFormat(stackImage)) {
            LOGGER.debug("Launching with Azure Marketplace image {}", stackImage);
            AzureMarketplaceImage azureMarketplaceImage = azureMarketplaceImageProviderService.get(stackImage);
            signIfAllowed(stack, azureCredentialView, azureMarketplaceImage, client);
            template = azureTemplateBuilder.build(stackName, null, azureCredentialView, azureStackView,
                    cloudContext, stack, AzureInstanceTemplateOperation.PROVISION, azureMarketplaceImage);
        } else {
            LOGGER.debug("Launching with non-Azure Marketplace image {}", stackImage);
            AzureImage image = azureStorage.getCustomImage(client, ac, stack);
            if (!image.getAlreadyExists()) {
                LOGGER.debug("Image {} has been created now, so we need to persist it", image.getName());
                CloudResource imageCloudResource =
                        azureCloudResourceService.buildCloudResource(image.getName(), image.getId(), ResourceType.AZURE_MANAGED_IMAGE);
                azureCloudResourceService.saveCloudResources(notifier, ac.getCloudContext(), List.of(imageCloudResource));
            }
            String customImageId = image.getId();
            boolean hasSourceImagePlan = azureImageFormatValidator.hasSourceImagePlan(stackImage);
            signSourceImageIfExists(stack, azureCredentialView, client, stackImage, hasSourceImagePlan);
            template = azureTemplateBuilder.build(stackName, customImageId, azureCredentialView, azureStackView,
                    cloudContext, stack, AzureInstanceTemplateOperation.PROVISION, hasSourceImagePlan ?
                            azureMarketplaceImageProviderService.getSourceImage(stackImage) : null);
        }

        String parameters = azureTemplateBuilder.buildParameters();

        boolean resourcesPersisted = false;
        try {
            List<CloudResource> instances;
            if (shouldCreateTemplateDeployment(stackName, resourceGroupName, client)) {
                Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
                LOGGER.debug("Created template deployment for launch: {}", templateDeployment.exportTemplate().template());
                instances = persistCloudResources(ac, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
            } else {
                Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
                LOGGER.debug("Get template deployment for launch as it exists: {}", templateDeployment.exportTemplate().template());
                instances = persistCloudResources(ac, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
            }
            resourcesPersisted = true;
            Network network = stack.getNetwork();
            List<String> subnetNameList = azureUtils.getCustomSubnetIds(network);

            List<CloudResource> networkResources = azureCloudResourceService.collectAndSaveNetworkAndSubnet(
                    resourceGroupName, stackName, notifier, cloudContext, subnetNameList, network, client);
            azureComputeResourceService.buildComputeResourcesForLaunch(ac, stack, adjustmentTypeWithThreshold, instances, networkResources);
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Stack provisioning");
        } catch (Exception e) {
            LOGGER.warn("Provisioning error:", e);
            throw new CloudConnectorException(String.format("Error in provisioning stack %s: %s", stackName, e.getMessage()));
        } finally {
            if (!resourcesPersisted) {
                Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
                if (templateDeployment != null && templateDeployment.exportTemplate() != null) {
                    LOGGER.debug("Get template deployment to persist created resources: {}", templateDeployment.exportTemplate().template());
                    persistCloudResources(ac, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
                }
            }
        }

        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.ARM_TEMPLATE)
                .withName(resourceGroupName)
                .build();
        List<CloudResourceStatus> resources = check(ac, Collections.singletonList(cloudResource));
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    private void signIfAllowed(CloudStack stack, AzureCredentialView azureCredentialView, AzureMarketplaceImage azureMarketplaceImage, AzureClient client) {
        Boolean automaticTermsAcceptance = Boolean.valueOf(stack.getParameters().get(ACCEPTANCE_POLICY_PARAMETER));
        if (automaticTermsAcceptance) {
            azureImageTermsSignerService.sign(azureCredentialView.getSubscriptionId(), azureMarketplaceImage, client);
        } else {
            LOGGER.debug("Azure automatic image term signing skipped: [automaticTermsAcceptancePolicy={}]", automaticTermsAcceptance);
        }
    }

    private void signSourceImageIfExists(CloudStack stack, AzureCredentialView azureCredentialView, AzureClient client, Image stackImage,
            boolean hasSourceImagePlan) {
        try {
            if (hasSourceImagePlan) {
                AzureMarketplaceImage sourceImage = azureMarketplaceImageProviderService.getSourceImage(stackImage);
                LOGGER.debug("Image has a source image plan, attempting to sign source image: {}", sourceImage.toString());
                signIfAllowed(stack, azureCredentialView, sourceImage, client);
            }
        } catch (CloudImageException e) {
            LOGGER.debug("Failed to sign source image: {}. Unboxing exception, because we have no fallback path for this case.", e.getMessage());
            throw new CloudConnectorException(e.getMessage());
        }
    }

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier notifier) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        AzureCredentialView azureCredentialView = new AzureCredentialView(authenticatedContext.getCloudCredential());
        AzureStackView azureStackView = azureStackViewProvider.getAzureStack(azureCredentialView, stack, client, authenticatedContext);
        String template = azureTemplateBuilder.buildLoadBalancer(stackName, azureCredentialView, azureStackView, cloudContext, stack,
                AzureInstanceTemplateOperation.PROVISION);
        if (!StringUtils.hasText(template)) {
            LOGGER.info("Load balancer template is null. Only FreeIPA load balancer is deployed through it's own template. Nothing to do here.");
            return ImmutableList.of();
        } else {
            createLbTemplateDeployment(authenticatedContext, stack, notifier, stackName, resourceGroupName, template);

            CloudResource cloudResource = CloudResource.builder()
                    .withType(ResourceType.ARM_TEMPLATE)
                    .withName(resourceGroupName)
                    .build();
            List<CloudResourceStatus> resources = check(authenticatedContext, Collections.singletonList(cloudResource));
            LOGGER.debug("Launched Load Balancer resources: {}", resources);
            return resources;
        }
    }

    @Override
    public List<CloudResourceStatus> updateLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        LOGGER.debug("Updating loadbalancer");
        return launchLoadBalancers(authenticatedContext, stack, persistenceNotifier);
    }

    public void deleteLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, List<String> loadBalancersToRemove) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        azureUtils.deleteLoadBalancers(client, resourceGroupName, loadBalancersToRemove);
    }

    @Override
    public List<CloudLoadBalancer> describeLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudLoadBalancerMetadata> loadBalancers) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        return azureUtils.describeLoadBalancers(client, resourceGroupName, loadBalancers);
    }

    @Override
    public void detachPublicIpAddressesForVMsIfNotPrivate(AuthenticatedContext authenticatedContext, CloudStack stack) {
        if (!azureUtils.isPrivateIp(stack.getNetwork())) {
            LOGGER.info("Stack has public IPs, we will detach public IP addresses from the VMs.");
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            AzureCredentialView azureCredentialView = new AzureCredentialView(authenticatedContext.getCloudCredential());
            AzureStackView azureStackView = azureStackViewProvider.getAzureStack(azureCredentialView, stack, client, authenticatedContext);
            CloudContext cloudContext = authenticatedContext.getCloudContext();
            String stackName = azureUtils.getStackName(cloudContext);
            String template = azureTemplateBuilder.buildPublicIpDetachForVMs(stackName, cloudContext, azureStackView, stack);
            String parameters = azureTemplateBuilder.buildParameters();
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
            LOGGER.info("Template for detaching public IP addresses from the VMs: {}", template);
            client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
        }
    }

    @Override
    public List<CloudResource> attachPublicIpAddressesForVMsAndAddLB(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier notifier) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        AzureCredentialView azureCredentialView = new AzureCredentialView(authenticatedContext.getCloudCredential());
        AzureStackView azureStackView = azureStackViewProvider.getAzureStack(azureCredentialView, stack, client, authenticatedContext);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String template = azureTemplateBuilder.buildAttachPublicIpsForVMsAndAddLB(stackName, cloudContext, azureCredentialView, azureStackView, stack);
        String parameters = azureTemplateBuilder.buildParameters();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        LOGGER.info("Template for attaching public IP addresses and add LBs: {}", template);
        Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
        persistCloudResources(authenticatedContext, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
        return azureCloudResourceService.getDeploymentCloudResources(templateDeployment);
    }

    private void createLbTemplateDeployment(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier notifier, String stackName,
            String resourceGroupName, String template) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        boolean resourcesPersisted = false;
        try {
            if (shouldCreateTemplateDeployment(stackName, resourceGroupName, client)) {
                String parameters = azureTemplateBuilder.buildParameters();
                Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
                LOGGER.debug("Created template deployment for Load Balancer launch: {}", templateDeployment.exportTemplate().template());
                persistCloudResources(authenticatedContext, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
            } else {
                Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
                LOGGER.debug("Get template deployment for Load Balancer launch as it exists: {}", templateDeployment.exportTemplate().template());
                persistCloudResources(authenticatedContext, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
            }
            resourcesPersisted = true;
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Load Balancer provisioning");
        } catch (Exception e) {
            LOGGER.warn("Load Balancer Provisioning error:", e);
            throw new CloudConnectorException(String.format("Error in provisioning load balancer %s: %s", stackName, e.getMessage()));
        } finally {
            if (!resourcesPersisted) {
                Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
                if (templateDeployment != null && templateDeployment.exportTemplate() != null) {
                    LOGGER.debug("Get template deployment to persist created resources: {}", templateDeployment.exportTemplate().template());
                    persistCloudResources(authenticatedContext, stack, notifier, cloudContext, stackName, resourceGroupName, templateDeployment);
                }
            }
        }
    }

    private boolean shouldCreateTemplateDeployment(String stackName, String resourceGroupName, AzureClient client) {
        return !client.templateDeploymentExists(resourceGroupName, stackName) ||
                client.getTemplateDeploymentStatus(resourceGroupName, stackName) != ResourceStatus.IN_PROGRESS;
    }

    private List<CloudResource> persistCloudResources(
            AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier, CloudContext cloudContext, String stackName,
            String resourceGroupName, Deployment templateDeployment) {
        List<CloudResource> allResourcesToPersist = new ArrayList<>();
        List<CloudResource> instances;
        try {
            List<CloudResource> templateResources = azureCloudResourceService.getDeploymentCloudResources(templateDeployment);
            LOGGER.debug("Template resources retrieved: {} for {}", templateResources, stackName);
            allResourcesToPersist.addAll(templateResources);
            instances = azureCloudResourceService.getInstanceCloudResources(stackName, templateResources, stack.getGroups(), resourceGroupName);
            if (!instances.isEmpty()) {
                allResourcesToPersist.addAll(collectOsDisks(ac, resourceGroupName, instances));
            } else {
                LOGGER.warn("Skipping OS disk collection as there was no VM instance found amongst cloud resources for {}!", stackName);
            }
        } finally {
            azureCloudResourceService.deleteCloudResources(notifier, cloudContext, allResourcesToPersist);
            azureCloudResourceService.saveCloudResources(notifier, cloudContext, allResourcesToPersist);
            LOGGER.info("Resources persisted: {}", allResourcesToPersist);
        }
        return instances;
    }

    private List<CloudResource> collectOsDisks(AuthenticatedContext ac, String resourceGroupName, List<CloudResource> instances) {
        AzureClient azureClient = ac.getParameter(AzureClient.class);
        List<CloudResource> osDiskResources = azureCloudResourceService.getAttachedOsDiskResources(instances, resourceGroupName, azureClient);
        LOGGER.debug("OS disk resources retrieved: {}", osDiskResources);
        return osDiskResources;
    }

    @Override
    public void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, TargetMajorVersion targetMajorVersion) {
        azureDatabaseResourceService.validateUpgradeDatabaseServer(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> launchValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            TargetMajorVersion targetMajorVersion, DatabaseStack migratedDbStack, PersistenceNotifier persistenceNotifier) {
        return azureDatabaseResourceService.launchCanaryDatabaseForUpgrade(
                authenticatedContext, stack, migratedDbStack, persistenceNotifier);
    }

    @Override
    public void cleanupValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack, List<CloudResource> resources,
            PersistenceNotifier persistenceNotifier) {
        azureDatabaseResourceService.deleteCanaryDatabaseForUpgrade(authenticatedContext, persistenceNotifier, resources);
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) {
        return azureDatabaseResourceService.buildDatabaseResourcesForLaunch(authenticatedContext, stack, persistenceNotifier);
    }

    @Override
    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        return azureDatabaseResourceService.getDatabaseServerStatus(authenticatedContext, stack);
    }

    @Override
    public ExternalDatabaseParameters getDatabaseServerParameters(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        return azureDatabaseResourceService.getExternalDatabaseParameters(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());

        providerResourceSyncers.stream()
                .filter(syncer -> syncer.platform().equals(authenticatedContext.getCloudContext().getPlatform()))
                .forEach(syncer -> result.addAll(syncer.sync(authenticatedContext, resources)));

        for (CloudResource resource : resources) {
            ResourceType resourceType = resource.getType();
            if (resourceType == ResourceType.ARM_TEMPLATE) {
                LOGGER.debug("Checking Azure stack status of: {}", stackName);
                checkTemplateDeployment(result, client, stackName, resource);
            } else {
                if (!resourceType.name().startsWith("AZURE")) {
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resourceType));
                }
            }
        }
        return result;
    }

    private void checkTemplateDeployment(List<CloudResourceStatus> result, AzureClient client, String stackName, CloudResource resource) {
        try {
            String resourceGroupName = resource.getName();
            CloudResourceStatus templateResourceStatus;
            if (client.templateDeploymentExists(resourceGroupName, stackName)) {
                Deployment resourceGroupDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
                templateResourceStatus = azureUtils.getTemplateStatus(resource, resourceGroupDeployment, client, stackName);
            } else {
                templateResourceStatus = new CloudResourceStatus(resource, ResourceStatus.DELETED);
            }
            result.add(templateResourceStatus);
        } catch (ManagementException e) {
            if (azureExceptionHandler.isNotFound(e)) {
                result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED));
            } else {
                throw new CloudConnectorException(e.getValue().getMessage(), e);
            }
        } catch (RuntimeException e) {
            throw new CloudConnectorException(String.format("Invalid resource exception: %s", e.getMessage()), e);
        }
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);
        ResourceGroupUsage resourceGroupUsage = azureResourceGroupMetadataProvider.getResourceGroupUsage(stack);

        if (resourceGroupUsage != ResourceGroupUsage.MULTIPLE) {

            String deploymentName = azureUtils.getStackName(ac.getCloudContext());
            List<CloudResource> transientResources = azureTerminationHelperService.handleTransientDeployment(client, resourceGroupName, deploymentName);
            NullUtil.doIfNotNull(transientResources, resources::addAll);
            azureTerminationHelperService.terminate(ac, stack, resources);
            return check(ac, Collections.emptyList());
        } else {
            try {
                try {
                    azureUtils.checkResourceGroupExistence(client, resourceGroupName);
                    client.deleteResourceGroup(resourceGroupName);
                } catch (ActionFailedException ignored) {
                    LOGGER.debug("Resource group not found with name: {}", resourceGroupName);
                }
            } catch (ManagementException e) {
                if (!azureExceptionHandler.isNotFound(e)) {
                    throw new CloudConnectorException(String.format("Could not delete resource group: %s", resourceGroupName), e);
                } else {
                    return check(ac, Collections.emptyList());
                }
            }
            return check(ac, resources);
        }
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) {

        azureDatabaseResourceService.handleTransientDeployment(authenticatedContext, resources);
        return azureDatabaseResourceService.terminateDatabaseServer(authenticatedContext, stack, resources, force, persistenceNotifier);
    }

    @Override
    public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        azureDatabaseResourceService.startDatabaseServer(authenticatedContext, stack);
    }

    @Override
    public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        azureDatabaseResourceService.stopDatabaseServer(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            UpdateType type, Optional<String> group) throws QuotaExceededException {
        LOGGER.info("The update method which will be followed is {}.", type);
        if (type.equals(UpdateType.VERTICAL_SCALE)) {
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            AzureStackView azureStackView = azureStackViewProvider
                    .getAzureStack(new AzureCredentialView(authenticatedContext.getCloudCredential()), stack, client, authenticatedContext);
            return azureVerticalScaleService.verticalScale(authenticatedContext, stack, resources, azureStackView, client, group);
        }
        return List.of();
    }

    @Override
    public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            Map<InstanceGroupType, String> userData) {
        LOGGER.info("Update userdata is not implemented on Azure!");
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws QuotaExceededException {
        AzureClient client = ac.getParameter(AzureClient.class);
        AzureStackView azureStackView = azureStackViewProvider.getAzureStack(new AzureCredentialView(ac.getCloudCredential()), stack, client, ac);
        return azureUpscaleService.upscale(ac, stack, resources, azureStackView, client, adjustmentTypeWithThreshold);
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudResource> resourcesToRemove) {
        return azureTerminationHelperService.downscale(ac, stack, vms, resources, resourcesToRemove);
    }

    @Override
    public List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {

        List<CloudResource> result = Lists.newArrayList();

        result.addAll(getDeletableResources(resources, vms));
        result.addAll(collectProviderSpecificResources(resources, vms));
        return result;
    }

    @Override
    protected Collection<CloudResource> getDeletableResources(Iterable<CloudResource> resources, Iterable<CloudInstance> instances) {
        Collection<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            String instanceId = instance.getInstanceId();
            if (instanceId != null) {
                for (CloudResource resource : resources) {
                    if (instanceId.equalsIgnoreCase(resource.getName()) || instanceId.equalsIgnoreCase(resource.getInstanceId())) {
                        result.add(resource);
                    }
                }
            }
        }
        LOGGER.debug("Collected deletable resources for downscale are: {}", result);
        return result;
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        return List.of();
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() {
        return azureTemplateBuilder.getTemplateString();
    }

    @Override
    public String getDBStackTemplate(DatabaseStack databaseStack) {
        return azureDatabaseResourceService.getDBStackTemplate(databaseStack);
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    @Override
    public void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack originalStack, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) {
        azureDatabaseResourceService.upgradeDatabaseServer(authenticatedContext, originalStack, stack, persistenceNotifier, targetMajorVersion, resources);
    }

    @Override
    public void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword) {
        azureDatabaseResourceService.updateAdministratorLoginPassword(authenticatedContext, databaseStack, newPassword);
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return ResourceType.AZURE_INSTANCE;
    }
}