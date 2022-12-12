package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
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

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, AzureStackView azureStackView,
            AzureClient client, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws QuotaExceededException {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        List<CloudResource> newInstances = new ArrayList<>();
        List<CloudResource> templateResources = new ArrayList<>();
        List<CloudResource> osDiskResources = new ArrayList<>();

        DateTime preDeploymentTime = DateTime.now();
        filterExistingInstances(azureStackView);
        try {
            List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
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

            azureCloudResourceService.saveCloudResources(resourceNotifier, cloudContext, ListUtils.union(templateResources, osDiskResources));

            List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(resources, newInstances);
            List<CloudResource> networkResources = azureCloudResourceService.getNetworkResources(resources);

            azureComputeResourceService.buildComputeResourcesForUpscale(ac, stack, scaledGroups, newInstances, reattachableVolumeSets, networkResources,
                    adjustmentTypeWithThreshold);

            List<CloudResourceStatus> successfulInstances = newInstances.stream()
                    .map(cloudResource ->
                            new CloudResourceStatus(cloudResource, ResourceStatus.CREATED, cloudResource.getParameter(CloudResource.PRIVATE_ID, Long.class)))
                    .collect(Collectors.toList());
            return ListUtils.union(Collections.singletonList(new CloudResourceStatus(armTemplate, ResourceStatus.IN_PROGRESS)),
                    successfulInstances);
        } catch (Retry.ActionFailedException e) {
            LOGGER.error("Retry.ActionFailedException happened", e);
            azureScaleUtilService.rollbackResources(ac, client, stack, cloudContext, resources, preDeploymentTime);
            throw azureUtils.convertToCloudConnectorException(e.getCause(), "Stack upscale");
        } catch (CloudException e) {
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

    private void filterExistingInstances(AzureStackView azureStackView) {
        azureStackView.getInstancesByGroupType().forEach((key, value) -> value.removeIf(AzureInstanceView::hasRealInstanceId));
        azureStackView.getInstancesByGroupType().entrySet().removeIf(group -> group.getValue() == null || group.getValue().isEmpty());
    }

    private List<CloudResource> getReattachableVolumeSets(List<CloudResource> resources, List<CloudResource> newInstances) {
        return resources.stream()
                .filter(cloudResource -> ResourceType.AZURE_VOLUMESET.equals(cloudResource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus())
                        || newInstances.stream().anyMatch(inst -> inst.getInstanceId().equals(cloudResource.getInstanceId())))
                .collect(Collectors.toList());
    }

}
