package com.sequenceiq.cloudbreak.cloud.azure.client;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.OperatingSystemStateTypes;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureManagedImageService;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationPoller;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
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
    private AzureImageIdProviderService azureImageIdProviderService;

    @Inject
    private AzureManagedImageCreationPoller azureManagedImageCreationPoller;

    @Inject
    private AzureManagedImageService azureManagedImageService;

    @Inject
    private AzureAuthExceptionHandler azureAuthExceptionHandler;

    public AzureImage getCustomImageId(String resourceGroup, String fromVhdUri, AuthenticatedContext ac, boolean createIfNotFound, AzureClient client) {
        String region = getRegion(ac);
        String imageName = getImageName(fromVhdUri, region);
        String imageId = getImageId(resourceGroup, client, imageName);
        AzureManagedImageCreationCheckerContext checkerContext = new AzureManagedImageCreationCheckerContext(client, resourceGroup, imageName);

        if (getCustomImage(resourceGroup, client, imageName).isPresent() || isRequested(imageId)) {
            LOGGER.debug("Custom image found in '{}' resource group with name '{}'", resourceGroup, imageName);
            azureManagedImageCreationPoller.startPolling(ac, checkerContext);
            return new AzureImage(imageId, imageName, true);
        } else {
            LOGGER.debug("Custom image NOT found in '{}' resource group with name '{}', creating it now: {}", resourceGroup, imageName, createIfNotFound);
            if (createIfNotFound) {
                saveImage(ac, imageName, imageId);
                Optional<VirtualMachineCustomImage> customImage;
                try {
                    customImage = Optional.of(createCustomImage(imageName, resourceGroup, fromVhdUri, region, client));
                } catch (CloudException e) {
                    customImage = handleImageCreationException(resourceGroup, ac, client, imageName, imageId, checkerContext, e);
                }
                return customImage.map(image -> createNewAzureImage(ac, image))
                        .orElseThrow(() -> new CloudConnectorException("Failed to create custom image."));
            } else {
                return null;
            }
        }
    }

    private AzureImage createNewAzureImage(AuthenticatedContext ac, VirtualMachineCustomImage customImage) {
        String imageName = customImage.id();
        String imageId = customImage.name();
        updateImageStatus(ac, imageName, imageId, CommonStatus.CREATED);
        return new AzureImage(imageId, imageName, true);
    }

    private Optional<VirtualMachineCustomImage> handleImageCreationException(String resourceGroup, AuthenticatedContext ac, AzureClient client,
            String imageName, String imageId, AzureManagedImageCreationCheckerContext checkerContext, CloudException e) {
        Optional<VirtualMachineCustomImage> customImage;
        azureManagedImageCreationPoller.startPolling(ac, checkerContext);
        customImage = getCustomImage(resourceGroup, client, imageName);
        if (customImage.isEmpty()) {
            LOGGER.error("Failed to create custom image.", e);
            updateImageStatus(ac, imageName, imageId, CommonStatus.FAILED);
            throw new CloudConnectorException(e);
        }
        return customImage;
    }

    private Optional<VirtualMachineCustomImage> getCustomImage(String resourceGroup, AzureClient client, String imageName) {
        return azureManagedImageService.findVirtualMachineCustomImage(resourceGroup, imageName, client);
    }

    private String getImageName(String fromVhdUri, String region) {
        String vhdName = fromVhdUri.substring(fromVhdUri.lastIndexOf('/') + 1);
        return CustomVMImageNameProvider.get(region, vhdName);
    }

    private void saveImage(AuthenticatedContext ac, String imageName, String imageId) {
        LOGGER.debug("Persisting image with REQUESTED status: {}", imageId);
        persistenceNotifier.notifyAllocation(buildCloudResource(imageName, imageId, CommonStatus.REQUESTED), ac.getCloudContext());
    }

    private void updateImageStatus(AuthenticatedContext ac, String imageName, String imageId, CommonStatus commonStatus) {
        LOGGER.debug("Updating image status to CREATED: {}", imageId);
        persistenceNotifier.notifyUpdate(buildCloudResource(imageName, imageId, commonStatus), ac.getCloudContext());
    }

    private String getRegion(AuthenticatedContext ac) {
        return ac.getCloudContext()
                .getLocation()
                .getRegion()
                .getRegionName();
    }

    private String getImageId(String resourceGroup, AzureClient client, String imageName) {
        return azureImageIdProviderService.generateImageId(client.getCurrentSubscription()
                .subscriptionId(), resourceGroup, imageName);
    }

    public CloudResource buildCloudResource(String name, String id, CommonStatus status) {
        return CloudResource.builder()
                .name(name)
                .status(status)
                .persistent(true)
                .reference(id)
                .type(ResourceType.AZURE_MANAGED_IMAGE)
                .build();
    }

    private boolean isRequested(String imageId) {
        return getImagesFromDatabase(imageId).isPresent();
    }

    private Optional<CloudResource> getImagesFromDatabase(String imageId) {
        return resourcePersistenceRetriever.notifyRetrieve(imageId, CommonStatus.REQUESTED, ResourceType.AZURE_MANAGED_IMAGE);
    }

    private VirtualMachineCustomImage createCustomImage(String imageName, String resourceGroup, String fromVhdUri, String region, AzureClient client) {
        return handleAuthException(() -> {
            Azure azure = client.getAzure();
            LOGGER.info("check the existence of resource group '{}', creating if it doesn't exist on Azure side", resourceGroup);
            if (!azure.resourceGroups().contain(resourceGroup)) {
                LOGGER.info("Creating resource group: {}", resourceGroup);
                azure.resourceGroups()
                        .define(resourceGroup)
                        .withRegion(region)
                        .create();
            }
            LOGGER.debug("Create custom image from '{}' with name '{}' into '{}' resource group (Region: {})",
                    fromVhdUri, imageName, resourceGroup, region);
            return measure(() -> azure.virtualMachineCustomImages()
                    .define(imageName)
                    .withRegion(region)
                    .withExistingResourceGroup(resourceGroup)
                    .withLinuxFromVhd(fromVhdUri, OperatingSystemStateTypes.GENERALIZED)
                    .create(),
                    LOGGER, "Custom image has been created under {} ms with name {}", imageName);
        });
    }

    private <T> T handleAuthException(Supplier<T> function) {
        return azureAuthExceptionHandler.handleAuthException(function);
    }
}
