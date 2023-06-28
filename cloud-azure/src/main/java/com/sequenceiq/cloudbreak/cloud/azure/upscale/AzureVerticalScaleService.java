package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVirtualMachineService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AzureVerticalScaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVerticalScaleService.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    public List<CloudResourceStatus> verticalScale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, AzureStackView azureStackView,
        AzureClient client, Optional<String> groupName) throws QuotaExceededException {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        try {
            List<CloudInstance> instanceList = azureUtils.getInstanceList(stack);
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
            Map<String, VirtualMachine> virtualMachinesByName = azureVirtualMachineService.getVirtualMachinesByName(
                    client,
                    resourceGroupName,
                    instanceList.stream().map(CloudInstance::getInstanceId).collect(Collectors.toSet()));

            for (CloudInstance cloudInstance : instanceList) {
                virtualMachinesByName.values()
                        .stream()
                        .filter(value -> value.name().equals(cloudInstance.getInstanceId()))
                        .filter(value -> !value.size().toString().equalsIgnoreCase(cloudInstance.getTemplate().getFlavor()))
                        .forEach(value ->  {
                            LOGGER.info("Vertical scaling vm {} with flavor {}.", value.name(), cloudInstance.getTemplate().getFlavor());
                            client.modifyInstanceType(resourceGroupName, value.name(), cloudInstance.getTemplate().getFlavor());
                        });
            }
            return List.of();
        } catch (Exception e) {
            LOGGER.error("Exception happened", e);
            throw new CloudConnectorException(String.format("Could not upscale Azure infrastructure: %s, %s", stackName,
                    e.getMessage()), e);
        }
    }

}
