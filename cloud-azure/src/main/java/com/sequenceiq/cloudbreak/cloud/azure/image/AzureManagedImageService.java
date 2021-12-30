package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.microsoft.azure.management.compute.ExecutionState.FAILED;
import static com.microsoft.azure.management.compute.ExecutionState.SUCCEEDED;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.compute.ExecutionState;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Service
public class AzureManagedImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureManagedImageService.class);

    public Optional<VirtualMachineCustomImage> findVirtualMachineCustomImage(AzureImageInfo azureImageInfo, AzureClient client) {
        String imageName = azureImageInfo.getImageNameWithRegion();
        String resourceGroup = azureImageInfo.getResourceGroup();
        VirtualMachineCustomImage image = client.findImage(resourceGroup, imageName);
        if (image != null && image.inner() != null) {
            return getImageByState(imageName, resourceGroup, image);
        } else {
            LOGGER.debug("Custom image {} is not present in resource group {}", imageName, resourceGroup);
            return Optional.empty();
        }
    }

    private Optional<VirtualMachineCustomImage> getImageByState(String imageName, String resourceGroup, VirtualMachineCustomImage image) {
        ExecutionState creationState = ExecutionState.fromString(image.inner().provisioningState());
        if (creationState == SUCCEEDED) {
            LOGGER.debug("Custom image {} is present in resource group {}", imageName, resourceGroup);
            return Optional.of(image);
        } else if (creationState == FAILED) {
            throw new CloudConnectorException(
                    String.format("Image (%s/%s) creation failed in Azure, please check the reason on Azure Portal!", resourceGroup, imageName));
        } else {
            LOGGER.debug("Custom image {} in resource group {} has non final status {}", imageName, resourceGroup, creationState.toString());
            return Optional.empty();
        }
    }
}
