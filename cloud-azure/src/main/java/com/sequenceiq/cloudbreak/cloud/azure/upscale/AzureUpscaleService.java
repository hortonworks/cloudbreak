package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
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
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceConnector.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AzureComputeResourceService azureComputeResourceService;

    @Inject
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, AzureStackView azureStackView,
            AzureClient client) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureUtils.getResourceGroupName(cloudContext, stack);

        try {
            List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
            CloudResource armTemplate = getArmTemplate(resources, stackName);
            Map<String, AzureDiskType> storageAccounts = azureStackView.getStorageAccounts();
            String region = cloudContext.getLocation().getRegion().value();
            for (Map.Entry<String, AzureDiskType> entry : storageAccounts.entrySet()) {
                azureStorage.createStorage(client, entry.getKey(), entry.getValue(), resourceGroupName, region, isEncryptionNeeded(stack), stack.getTags());
            }
            purgeExistingInstances(azureStackView);
            Deployment templateDeployment = azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView);
            LOGGER.info("Created template deployment for upscale: {}", templateDeployment.exportTemplate().template());
            List<CloudResource> newInstances = azureUtils.getInstanceCloudResources(cloudContext, templateDeployment, scaledGroups);
            List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(resources);
            List<CloudResource> networkResources = cloudResourceHelper.getNetworkResources(resources);
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
        azureStackView.getGroups().entrySet().removeIf(group -> group.getValue() == null || group.getValue().size() == 0);
    }

    private CloudResource getArmTemplate(List<CloudResource> resources, String stackName) {
        return resources.stream().filter(r -> r.getType() == ResourceType.ARM_TEMPLATE).findFirst()
                .orElseThrow(() -> new CloudConnectorException(String.format("Arm Template not found for: %s  ", stackName)));
    }

    private Boolean isEncryptionNeeded(CloudStack stack) {
        return azureStorage.isEncrytionNeeded(stack.getParameters());
    }

    private List<CloudResource> getReattachableVolumeSets(List<CloudResource> resources) {
        return resources.stream()
                .filter(cloudResource -> ResourceType.AZURE_VOLUMESET.equals(cloudResource.getType()))
                .filter(cloudResource -> Objects.isNull(cloudResource.getInstanceId()))
                .collect(Collectors.toList());
    }
}
