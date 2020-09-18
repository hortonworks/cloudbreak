package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;

@Service
public class AzureManagedImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureManagedImageService.class);

    @Inject
    private AzureAuthExceptionHandler azureAuthExceptionHandler;

    public Optional<VirtualMachineCustomImage> findVirtualMachineCustomImage(String resourceGroup, String imageName, AzureClient client) {
        VirtualMachineCustomImage image = findImage(resourceGroup, imageName, client);
        if (image != null) {
            LOGGER.debug("Custom image {} is present in resource group {}", imageName, resourceGroup);
            return Optional.of(image);
        } else {
            LOGGER.debug("Custom image {} is not present in resource group {}", imageName, resourceGroup);
            return Optional.empty();
        }
    }

    private VirtualMachineCustomImage findImage(String resourceGroup, String imageName, AzureClient client) {
        LOGGER.debug("Searching custom image {} in resource group {}", imageName, resourceGroup);
        return azureAuthExceptionHandler.handleAuthException(() -> client.getAzure()
                .virtualMachineCustomImages()
                .getByResourceGroup(resourceGroup, imageName));
    }

    private <T> T handleAuthException(Supplier<T> function) {
        return azureAuthExceptionHandler.handleAuthException(function);
    }
}
