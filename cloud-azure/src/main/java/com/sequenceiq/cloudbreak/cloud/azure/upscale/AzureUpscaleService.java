package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUpscaleService.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AzureComputeResourceService azureComputeResourceService;

    @Inject
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private AzureScaleUtilService azureScaleUtilService;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    private AzureImageTermsSignerService azureImageTermsSignerService;

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, AzureStackView azureStackView,
            AzureClient client, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws QuotaExceededException {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        List<CloudResource> newInstances = new ArrayList<>();
        List<CloudResource> templateResources = new ArrayList<>();
        List<CloudResource> osDiskResources = new ArrayList<>();

        OffsetDateTime preDeploymentTime = OffsetDateTime.now();
        Image stackImage = stack.getImage();
        attemptToSignImageIfApplicable(ac, stack, client, stackImage);
        List<CloudResource> createdCloudInstances =
                resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, AZURE_INSTANCE, cloudContext.getId());
        LOGGER.debug("Created cloud instances: {}", createdCloudInstances);
        filterExistingInstances(stackName, azureStackView, createdCloudInstances);
        try {
            List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
            Map<String, Long> requestedInstancesPrivateIdMap = getRequestedInstancesPrivateIdMap(stackName, scaledGroups);
            CloudResource armTemplate = azureScaleUtilService.getArmTemplate(resources, stackName);

            Deployment templateDeployment =
                    azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE);
            LOGGER.info("Created template deployment for upscale: {}", templateDeployment.exportTemplate().template());

            templateResources.addAll(azureCloudResourceService.getDeploymentCloudResources(templateDeployment));
            newInstances.addAll(azureCloudResourceService.getInstanceCloudResources(stackName, templateResources,
                    scaledGroups, resourceGroupName));
            if (!newInstances.isEmpty()) {
                osDiskResources.addAll(azureCloudResourceService.getAttachedOsDiskResources(newInstances, resourceGroupName, client));
            } else {
                LOGGER.warn("Skipping OS disk collection as there was no VM instance found amongst cloud resources for {}!", stackName);
            }
            replaceAzureInstances(templateResources, newInstances);
            azureCloudResourceService.saveCloudResources(resourceNotifier, cloudContext, ListUtils.union(templateResources, osDiskResources));

            List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(resources, newInstances);
            List<CloudResource> networkResources = azureCloudResourceService.getNetworkResources(resources);
            createdCloudInstances.addAll(newInstances);
            List<CloudResource> createRequestedInstances = getRequestedInstancesWithPrivateId(createdCloudInstances, requestedInstancesPrivateIdMap);
            cloudResourceHelper.updateDeleteOnTerminationFlag(reattachableVolumeSets, false, ac.getCloudContext());
            azureComputeResourceService.buildComputeResourcesForUpscale(ac, stack, scaledGroups, createRequestedInstances, reattachableVolumeSets,
                    networkResources, adjustmentTypeWithThreshold);
            cloudResourceHelper.updateDeleteOnTerminationFlag(reattachableVolumeSets, true, ac.getCloudContext());

            List<CloudResourceStatus> successfulInstances = createRequestedInstances.stream()
                    .map(cloudResource ->
                            new CloudResourceStatus(cloudResource, ResourceStatus.CREATED, cloudResource.getParameter(PRIVATE_ID, Long.class)))
                    .collect(Collectors.toList());

            return ListUtils.union(Collections.singletonList(new CloudResourceStatus(armTemplate, ResourceStatus.IN_PROGRESS)),
                    successfulInstances);
        } catch (Retry.ActionFailedException e) {
            LOGGER.error("Retry.ActionFailedException happened", e);
            azureScaleUtilService.rollbackResources(ac, client, stack, cloudContext, resources, preDeploymentTime);
            throw azureUtils.convertToCloudConnectorException(e.getCause(), "Stack upscale");
        } catch (ManagementException e) {
            LOGGER.error("CloudException happened", e);
            azureScaleUtilService.rollbackResources(ac, client, stack, cloudContext, resources, preDeploymentTime);
            azureScaleUtilService.checkIfQuotaLimitIssued(e);
            throw azureUtils.convertToCloudConnectorException(e, "Stack upscale");
        } catch (RolledbackResourcesException e) {
            LOGGER.error("RolledbackResourcesException happened", e);
            azureScaleUtilService.rollbackResources(ac, client, stack, cloudContext, resources, preDeploymentTime);
            throw new CloudConnectorException(String.format("Could not upscale Azure infrastructure, infrastructure was rolled back with resources: %s, %s",
                    stackName, e.getMessage()), e);
        } catch (Exception e) {
            LOGGER.error("Exception happened", e);
            azureScaleUtilService.rollbackResources(ac, client, stack, cloudContext, resources, preDeploymentTime);
            throw new CloudConnectorException(String.format("Could not upscale Azure infrastructure, infrastructure was rolled back: %s, %s", stackName,
                    e.getMessage()), e);
        }
    }

    private void attemptToSignImageIfApplicable(AuthenticatedContext ac, CloudStack stack, AzureClient client, Image stackImage) {
        boolean hasSourceImagePlan = azureImageFormatValidator.hasSourceImagePlan(stackImage);
        if (azureImageFormatValidator.isMarketplaceImageFormat(stackImage) || hasSourceImagePlan) {
            AzureMarketplaceImage azureMarketplaceImage = azureImageFormatValidator.isMarketplaceImageFormat(stackImage) ?
                    azureMarketplaceImageProviderService.get(stackImage) : azureMarketplaceImageProviderService.getSourceImage(stackImage);
            AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());
            LOGGER.debug("Attempt to sign Azure Marketplace image {}", azureMarketplaceImage.toString());
            try {
                Boolean automaticTermsAcceptance = Boolean.valueOf(stack.getParameters().get(ACCEPTANCE_POLICY_PARAMETER));
                if (automaticTermsAcceptance) {
                    azureImageTermsSignerService.sign(azureCredentialView.getSubscriptionId(), azureMarketplaceImage, client);
                } else {
                    LOGGER.debug("Azure automatic image term signing skipped: [automaticTermsAcceptancePolicy={}]", automaticTermsAcceptance);
                }
            } catch (CloudImageException e) {
                if (hasSourceImagePlan) {
                    LOGGER.debug("Failed to sign source image: {}. Unboxing exception, because we have no fallback path for this case.", e.getMessage());
                    throw new CloudConnectorException(e.getMessage());
                } else {
                    LOGGER.debug("Failed to sign marketplace image: {}. Rethrowing to continue fallback handling.", e.getMessage());
                    throw e;
                }
            }
        }
    }

    private Map<String, Long> getRequestedInstancesPrivateIdMap(String stackName, List<Group> scaledGroups) {
        Map<String, Long> instanceIdPrivateIdMap = scaledGroups.stream().flatMap(group -> group.getInstances().stream())
                .filter(cloudInstance -> InstanceStatus.CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus()))
                .collect(Collectors.toMap(cloudInstance -> azureUtils.getFullInstanceId(stackName, cloudInstance.getTemplate().getGroupName(),
                                Long.toString(cloudInstance.getTemplate().getPrivateId()), cloudInstance.getDbIdOrDefaultIfNotExists()),
                        cloudInstance -> cloudInstance.getTemplate().getPrivateId()));
        LOGGER.debug("Instance id private id map: {}", instanceIdPrivateIdMap);
        return instanceIdPrivateIdMap;
    }

    private void replaceAzureInstances(List<CloudResource> templateResources, List<CloudResource> newInstances) {
        LOGGER.debug("Replace azure instances with the detailed instances");
        templateResources.removeIf(cloudResource -> AZURE_INSTANCE.equals(cloudResource.getType()));
        templateResources.addAll(newInstances);
        LOGGER.debug("Template resources after replacement: {}", templateResources);
    }

    private List<CloudResource> getRequestedInstancesWithPrivateId(List<CloudResource> createdCloudInstances, Map<String, Long> instanceIdPrivateIdMap) {
        return createdCloudInstances.stream()
                .filter(cloudResource -> instanceIdPrivateIdMap.containsKey(cloudResource.getInstanceId()))
                .map(cloudResource -> CloudResource.builder().cloudResource(cloudResource)
                        .withParameters(Map.of(PRIVATE_ID, instanceIdPrivateIdMap.get(cloudResource.getInstanceId())))
                        .build())
                .collect(Collectors.toList());
    }

    private void filterExistingInstances(String stackName, AzureStackView azureStackView, List<CloudResource> existingCloudInstances) {
        LOGGER.debug("Azure stack view before filtering existing instances {}", azureStackView);
        azureStackView.getInstancesByGroupType().forEach((key, value) -> value.removeIf(AzureInstanceView::hasRealInstanceId));
        azureStackView.getInstancesByGroupType().forEach((key, value) -> value.removeIf(azureInstanceView -> existingCloudInstances.stream()
                .anyMatch(cloudResource -> {
                    String instanceId = azureUtils.getFullInstanceId(stackName, azureInstanceView.getGroupName(),
                            Long.toString(azureInstanceView.getPrivateId()), azureInstanceView.getInstance().getDbIdOrDefaultIfNotExists());
                    boolean instanceIdFoundInExistingInstances = Objects.equals(cloudResource.getInstanceId(), instanceId);
                    if (instanceIdFoundInExistingInstances) {
                        LOGGER.debug("Instance id found in existing cloud instances: {}", instanceId);
                    }
                    return instanceIdFoundInExistingInstances;
                })));
        azureStackView.getInstancesByGroupType().entrySet().removeIf(group -> group.getValue() == null || group.getValue().isEmpty());
        LOGGER.debug("Azure stack view after filtering existing instances {}", azureStackView);
    }

    private List<CloudResource> getReattachableVolumeSets(List<CloudResource> resources, List<CloudResource> newInstances) {
        return resources.stream()
                .filter(cloudResource -> ResourceType.AZURE_VOLUMESET.equals(cloudResource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus())
                        || newInstances.stream().anyMatch(inst -> inst.getInstanceId().equals(cloudResource.getInstanceId())))
                .collect(Collectors.toList());
    }

}
