package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudError;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
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
    private AzureCloudResourceService azureCloudResourceService;

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, AzureStackView azureStackView,
            AzureClient client) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);

        try {
            List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
            CloudResource armTemplate = getArmTemplate(resources, stackName);
            purgeExistingInstances(azureStackView);
            Deployment templateDeployment =
                    azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE);
            LOGGER.info("Created template deployment for upscale: {}", templateDeployment.exportTemplate().template());

            List<CloudResource> templateResources = azureCloudResourceService.getDeploymentCloudResources(templateDeployment);
            List<CloudResource> newInstances =
                    azureCloudResourceService.getInstanceCloudResources(stackName, templateResources, scaledGroups, resourceGroupName);
            List<CloudResource> osDiskResources = azureCloudResourceService.getAttachedOsDiskResources(ac, newInstances, resourceGroupName);

            azureCloudResourceService.saveCloudResources(resourceNotifier, cloudContext, ListUtils.union(templateResources, osDiskResources));

            List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(resources, newInstances);
            List<CloudResource> networkResources = azureCloudResourceService.getNetworkResources(resources);

            azureComputeResourceService.buildComputeResourcesForUpscale(ac, stack, scaledGroups, newInstances, reattachableVolumeSets, networkResources);

            return Collections.singletonList(new CloudResourceStatus(armTemplate, ResourceStatus.IN_PROGRESS));
        } catch (CloudException e) {
            LOGGER.info("Upscale error, cloud exception happened: ", e);
            if (e.body() != null && e.body().details() != null) {
                String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                throw new CloudConnectorException(String.format("Stack upscale failed, status code %s, error message: %s, details: %s",
                        e.body().code(), e.body().message(), details));
            } else {
                throw new CloudConnectorException(String.format("Stack upscale failed: '%s', please go to Azure Portal for detailed message", e));
            }
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not upscale: %s  ", stackName), e);
        }
    }

    private void purgeExistingInstances(AzureStackView azureStackView) {
        azureStackView.getGroups().forEach((key, value) -> value.removeIf(AzureInstanceView::hasRealInstanceId));
        azureStackView.getGroups().entrySet().removeIf(group -> group.getValue() == null || group.getValue().isEmpty());
    }

    private CloudResource getArmTemplate(List<CloudResource> resources, String stackName) {
        return resources.stream().filter(r -> r.getType() == ResourceType.ARM_TEMPLATE).findFirst()
                .orElseThrow(() -> new CloudConnectorException(String.format("Arm Template not found for: %s  ", stackName)));
    }

    private List<CloudResource> getReattachableVolumeSets(List<CloudResource> resources, List<CloudResource> newInstances) {
        return resources.stream()
                .filter(cloudResource -> ResourceType.AZURE_VOLUMESET.equals(cloudResource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus())
                        || newInstances.stream().anyMatch(inst -> inst.getInstanceId().equals(cloudResource.getInstanceId())))
                .collect(Collectors.toList());
    }
}
