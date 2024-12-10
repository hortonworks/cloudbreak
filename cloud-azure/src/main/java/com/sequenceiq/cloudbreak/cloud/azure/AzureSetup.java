package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureHttpClientConfigurer;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageCopyDetails;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageSetupService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageFallbackException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ImageStatusResult;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class AzureSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSetup.class);

    private static final String TEST_CONTAINER = "cb-test-container";

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureImageSetupService azureImageSetupService;

    @Inject
    private AzureHttpClientConfigurer azureHttpClientConfigurer;

    @Override
    public void prepareImage(AuthenticatedContext ac, CloudStack stack, Image image, PrepareImageType prepareType, String fallbackTargetImage) {
        LOGGER.debug("Prepare image: {}", image);

        String region = ac.getCloudContext().getLocation().getRegion().value();
        AzureClient client = ac.getParameter(AzureClient.class);
        try {
            azureImageSetupService.copyVhdImageIfNecessary(ac, stack, image, region, client, prepareType, fallbackTargetImage);
        } catch (CloudImageFallbackException ex) {
            LOGGER.debug("Image fallback is necessary: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Could not create image with the specified parameters", ex);
            throwExceptionWithDetails(ac, stack, image, ex);
        }
        LOGGER.debug("Prepare image has been executed");
    }

    private void throwExceptionWithDetails(AuthenticatedContext ac, CloudStack stack, Image image, Exception ex) {
        Optional<AzureImageCopyDetails> imageCopyDetailsOptional = azureImageSetupService.getImageCopyDetails(ac, stack, image);
        if (imageCopyDetailsOptional.isPresent()) {
            AzureImageCopyDetails imageCopyDetails = imageCopyDetailsOptional.get();
            String message = String.format("%s image copy failed. You may try to execute the copy manually, in that case please copy %s to the 'images' " +
                            "container of the storage account '%s' in resource group '%s'. Reason of failure: %s - %s",
                    image.getImageName(), imageCopyDetails.getImageSource(), imageCopyDetails.getImageStorageName(),
                    imageCopyDetails.getImageResourceGroupName(), ex.getMessage(), ExceptionUtils.getRootCause(ex).getMessage());
            LOGGER.debug("Added details to exception: {}", message, ex);
            throw new CloudConnectorException(message, ex);
        } else {
            throw new CloudConnectorException(image.getImageName() + " image copy failed: " + ex.getMessage() + '.' +
                    ExceptionUtils.getRootCause(ex).getMessage(), ex);
        }
    }

    @Override
    public void validateImage(AuthenticatedContext auth, CloudStack stack, Image image) {
        LOGGER.debug("Validate image: {}", image);

        AzureClient client = auth.getParameter(AzureClient.class);
        azureImageSetupService.validateImage(auth, stack, image, client);
        LOGGER.debug("Validate image has been executed");
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext ac, CloudStack stack, Image image) {
        return azureImageSetupService.checkImageStatus(ac, stack, image);
    }

    @Override
    public void prerequisites(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);
        CloudResource cloudResource = CloudResource.builder().withType(ResourceType.ARM_TEMPLATE).withName(resourceGroupName).build();
        String region = ac.getCloudContext().getLocation().getRegion().value();
        try {
            AzureClient client = ac.getParameter(AzureClient.class);
            persistenceNotifier.notifyAllocation(cloudResource, ac.getCloudContext());
            if (!client.resourceGroupExists(resourceGroupName)) {
                client.createResourceGroup(resourceGroupName, region, stack.getTags());
            }
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateParameters(AuthenticatedContext ac, Map<String, String> parameters) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = parameters.get(PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER);
        ResourceGroupUsage resourceGroupUsage = getIfNotNull(
                parameters.get(PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER),
                ResourceGroupUsage::valueOf);

        if (Strings.isNullOrEmpty(resourceGroupName)) {
            return;
        }
        if (ResourceGroupUsage.MULTIPLE.equals(resourceGroupUsage)) {
            LOGGER.debug("Multiple RG mode is active, checking existence of resource group {}", resourceGroupName);
            ResourceGroup resourceGroup = client.getResourceGroup(resourceGroupName);
            if (resourceGroup != null) {
                throw new BadRequestException("Resource group name already exists: " + resourceGroup.name());
            }
        }
    }

    @Override
    public void scalingPrerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, boolean upscale) {

    }

    @Override
    public void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) {
        FileSystemType fileSystemType = spiFileSystem.getType();
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        if (cloudFileSystems.size() > 2) {
            throw new CloudConnectorException("More than 2 file systems (identities) are not yet supported on Azure!");
        }
        if (cloudFileSystems.isEmpty()) {
            LOGGER.info("No filesystem was configured.");
            return;
        }

        if (FileSystemType.ADLS_GEN_2.equals(fileSystemType)) {
            validateAdlsGen2FileSystem(spiFileSystem);
        } else {
            LOGGER.warn("Not supported file system.");
        }
    }

    private void validateAdlsGen2FileSystem(SpiFileSystem fileSystem) {
        try {
            CloudAdlsGen2View cloudFileSystem = (CloudAdlsGen2View) fileSystem.getCloudFileSystems().get(0);
            String accountName = cloudFileSystem.getAccountName();
            String accountKey = cloudFileSystem.getAccountKey();
            if (StringUtils.isEmpty(accountName)) {
                LOGGER.warn("Account name is empty. Ignoring ADLS_GEN2 validation.");
                return;
            }
            if (StringUtils.isEmpty(accountKey)) {
                LOGGER.warn("Account key is empty. Ignoring ADLS_GEN2 validation.");
                return;
            }
            BlobContainerClient blobContainerClient = azureHttpClientConfigurer.configureDefault(new BlobContainerClientBuilder())
                    .endpoint("https://" + accountName + ".blob.core.windows.net")
                    .containerName(TEST_CONTAINER + System.nanoTime())
                    .credential(new StorageSharedKeyCredential(accountName, accountKey))
                    .buildClient();
            blobContainerClient.createIfNotExists();
            blobContainerClient.delete();
        } catch (BlobStorageException e) {
            if (e.getCause() instanceof UnknownHostException) {
                throw new CloudConnectorException("The provided account does not belong to a valid storage account");
            }
        }
    }

}
