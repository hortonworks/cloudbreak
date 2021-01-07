package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            azureManagedImageCreationPoller.startPolling(ac, new AzureManagedImageCreationCheckerContext(azureImageInfo, client));
        } catch (Exception e) {
            LOGGER.warn("Exception when polling for existing cloudbreak image: ", e);
            throw new CloudConnectorException(e);
        }
        return Optional.of(new AzureImage(azureImageInfo.getImageId(), azureImageInfo.getImageNameWithRegion(), true));
    }

    public AzureImage createImage(AzureImageInfo azureImageInfo, String fromVhdUri, AzureClient client, AuthenticatedContext ac) {

        Optional<VirtualMachineCustomImage> customImage;
        AzureManagedImageCreationCheckerContext checkerContext = new AzureManagedImageCreationCheckerContext(azureImageInfo, client);
        try {
            saveImage(ac, azureImageInfo.getImageNameWithRegion(), azureImageInfo.getImageId());
            customImage = Optional.of(
                    client.createImage(azureImageInfo.getImageNameWithRegion(), azureImageInfo.getResourceGroup(), fromVhdUri, azureImageInfo.getRegion()));
        } catch (CloudException e) {
            LOGGER.warn("Exception when creating custom image", e);
            customImage = handleCustomImageCreationException(azureImageInfo, ac, client, checkerContext, e);

            // DataIntegrityViolationException is thrown if multiple parallel launches
            // would cause edge case of inserting multiple db record violating the unique constraint
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("Exception on saving image {} due to db unique constraint violation: {}",  azureImageInfo.getImageId(), e.getMessage());
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
            AzureClient client, AzureManagedImageCreationCheckerContext checkerContext, Exception originalException) {
        Exception onError = originalException;
        try {
            azureManagedImageCreationPoller.startPolling(ac, checkerContext);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout during polling for image creation: ", e);
        } catch (Exception e) {
            LOGGER.warn("Exception during polling for image creation: ", e);
            onError = e;
        } finally {
            Optional<VirtualMachineCustomImage> customImage;
            customImage = findImage(azureImageInfo, client);
            if (customImage.isEmpty()) {
                updateImageStatus(ac, azureImageInfo.getImageNameWithRegion(), azureImageInfo.getImageId(), CommonStatus.FAILED);
                LOGGER.error("Failed to create custom image, throwing: ", onError);
                throw new CloudConnectorException(onError);
            }
            return customImage;
        }
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
                .stackAware(false)
                .build();
    }

    private boolean isCreateRequested(AzureImageInfo azureImageInfo) {
        return resourcePersistenceRetriever.notifyRetrieve(azureImageInfo.getImageId(), CommonStatus.REQUESTED, ResourceType.AZURE_MANAGED_IMAGE).isPresent();
    }
}
