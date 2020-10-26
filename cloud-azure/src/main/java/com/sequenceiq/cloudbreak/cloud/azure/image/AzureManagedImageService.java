package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@Service
public class AzureManagedImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureManagedImageService.class);

    public Optional<VirtualMachineCustomImage> findVirtualMachineCustomImage(AzureImageInfo azureImageInfo, AzureClient client) {
        String imageName = azureImageInfo.getImageNameWithRegion();
        String resourceGroup = azureImageInfo.getResourceGroup();
        VirtualMachineCustomImage image = client.findImage(resourceGroup, imageName);
        if (image != null) {
            LOGGER.debug("Custom image {} is present in resource group {}", imageName, resourceGroup);
            return Optional.of(image);
        } else {
            LOGGER.debug("Custom image {} is not present in resource group {}", imageName, resourceGroup);
            return Optional.empty();
        }
    }
}
