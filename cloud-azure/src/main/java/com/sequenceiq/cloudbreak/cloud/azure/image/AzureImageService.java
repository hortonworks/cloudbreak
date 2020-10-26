package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageService.class);

    @Inject
    private PersistenceRetriever resourcePersistenceRetriever;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private AzureManagedImageCreationPoller azureManagedImageCreationPoller;

    @Inject
    private AzureManagedImageService azureManagedImageService;

    public Optional<AzureImage> findImage(AzureImageInfo azureImageInfo, AzureClient client, AuthenticatedContext ac) {
        if (findImage(azureImageInfo, client).isEmpty() && !isCreateRequested(azureImageInfo)) {
            return Optional.empty();
        }

        LOGGER.debug("Custom image found in '{}' resource group with name '{}'", azureImageInfo.getResourceGroup(), azureImageInfo.getImageNameWithRegion());
        azureManagedImageCreationPoller.startPolling(ac, new AzureManagedImageCreationCheckerContext(azureImageInfo, client));
        return Optional.of(new AzureImage(azureImageInfo.getImageId(), azureImageInfo.getImageNameWithRegion(), true));
    }

    public AzureImage createImage(AzureImageInfo azureImageInfo, String fromVhdUri, AzureClient client, AuthenticatedContext ac) {
        saveImage(ac, azureImageInfo.getImageNameWithRegion(), azureImageInfo.getImageId());
        Optional<VirtualMachineCustomImage> customImage;
        AzureManagedImageCreationCheckerContext checkerContext = new AzureManagedImageCreationCheckerContext(azureImageInfo, client);
        try {
            customImage = Optional.of(
                    client.createImage(azureImageInfo.getImageNameWithRegion(), azureImageInfo.getResourceGroup(), fromVhdUri, azureImageInfo.getRegion()));
        } catch (CloudException e) {
            customImage = handleCustomImageCreationException(azureImageInfo, ac, client, checkerContext, e);
        }
        return customImage
                .map(image -> createImageAndNotify(ac, image))
                .orElseThrow(() -> new CloudConnectorException("Failed to create custom image."));
    }

    private AzureImage createImageAndNotify(AuthenticatedContext ac, VirtualMachineCustomImage customImage) {
        updateImageStatus(ac, customImage.name(), customImage.id(), CommonStatus.CREATED);
        return new AzureImage(customImage.id(), customImage.name(), true);
    }

    private Optional<VirtualMachineCustomImage> handleCustomImageCreationException(AzureImageInfo azureImageInfo, AuthenticatedContext ac,
            AzureClient client, AzureManagedImageCreationCheckerContext checkerContext, CloudException e) {
        Optional<VirtualMachineCustomImage> customImage;
        azureManagedImageCreationPoller.startPolling(ac, checkerContext);
        customImage = findImage(azureImageInfo, client);
        if (customImage.isEmpty()) {
            LOGGER.error("Failed to create custom image.", e);
            updateImageStatus(ac, azureImageInfo.getImageNameWithRegion(), azureImageInfo.getImageId(), CommonStatus.FAILED);
            throw new CloudConnectorException(e);
        }
        return customImage;
    }

    private Optional<VirtualMachineCustomImage> findImage(AzureImageInfo azureImageInfo, AzureClient client) {
        return azureManagedImageService.findVirtualMachineCustomImage(azureImageInfo, client);
    }

    private void saveImage(AuthenticatedContext ac, String imageName, String imageId) {
        LOGGER.debug("Persisting image with REQUESTED status: {}", imageId);
        persistenceNotifier.notifyAllocation(buildCloudResource(imageName, imageId, CommonStatus.REQUESTED), ac.getCloudContext());
    }

    private void updateImageStatus(AuthenticatedContext ac, String imageName, String imageId, CommonStatus commonStatus) {
        LOGGER.debug("Updating image status to {}: {}", commonStatus, imageId);
        persistenceNotifier.notifyUpdate(buildCloudResource(imageName, imageId, commonStatus), ac.getCloudContext());
    }

    private CloudResource buildCloudResource(String name, String id, CommonStatus status) {
        return CloudResource.builder()
                .name(name)
                .status(status)
                .persistent(true)
                .reference(id)
                .type(ResourceType.AZURE_MANAGED_IMAGE)
                .build();
    }

    private boolean isCreateRequested(AzureImageInfo azureImageInfo) {
        return resourcePersistenceRetriever.notifyRetrieve(azureImageInfo.getImageId(), CommonStatus.REQUESTED, ResourceType.AZURE_MANAGED_IMAGE).isPresent();
    }
}
