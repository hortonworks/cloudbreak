package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES_CONTAINER;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView.TENANT_ID;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.datalake.store.DataLakeStoreAccountManagementClient;
import com.microsoft.azure.management.datalake.store.implementation.DataLakeStoreAccountManagementClientImpl;
import com.microsoft.azure.management.datalake.store.models.DataLakeStoreAccountBasic;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.AzurePrerequisiteDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisiteDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisitesCreateRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class AzureSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSetup.class);

    private static final String TEST_CONTAINER = "cb-test-container";

    @Inject
    private AzureStorage armStorage;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureUtils azureUtils;

    @Override
    public void prepareImage(AuthenticatedContext ac, CloudStack stack, Image image) {
        LOGGER.debug("Prepare image: {}", image);

        String region = ac.getCloudContext().getLocation().getRegion().value();
        AzureClient client = ac.getParameter(AzureClient.class);
        try {
            copyVhdImageIfNecessary(ac, stack, image, region, client);
        } catch (Exception ex) {
            LOGGER.warn("Could not create image with the specified parameters", ex);
            throw new CloudConnectorException(image.getImageName() + " image copy failed: " + ExceptionUtils.getRootCause(ex).getMessage(), ex);
        }
        LOGGER.debug("Prepare image has been executed");
    }

    private void copyVhdImageIfNecessary(AuthenticatedContext ac, CloudStack stack, Image image,
            String region, AzureClient client) {

        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), stack);
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(ac.getCloudContext(), stack);
        if (!client.resourceGroupExists(resourceGroupName)) {
            client.createResourceGroup(resourceGroupName, region, stack.getTags());
        }
        if (!client.resourceGroupExists(imageResourceGroupName)) {
            client.createResourceGroup(imageResourceGroupName, region, stack.getTags());
        }
        armStorage.createStorage(client, imageStorageName, AzureDiskType.LOCALLY_REDUNDANT, imageResourceGroupName, region,
                armStorage.isEncrytionNeeded(stack.getParameters()), stack.getTags());
        client.createContainerInStorage(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER);
        if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
            client.copyImageBlobInStorageContainer(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName());
        }
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext ac, CloudStack stack, Image image) {
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(ac.getCloudContext(), stack);
        AzureClient client = ac.getParameter(AzureClient.class);

        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), stack);
        try {
            CopyState copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName());
            if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
                if (StringUtils.isEmpty(armStorage.getCustomImageId(client, ac, stack))) {
                    return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED);
                }
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            } else if (CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus())) {
                return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
            } else {
                int percentage = (int) (((double) copyState.getBytesCopied() * ImageStatusResult.COMPLETED) / copyState.getTotalBytes());
                LOGGER.info("CopyStatus, Total:{} / Pending:{} bytes, {}%", copyState.getTotalBytes(), copyState.getBytesCopied(), percentage);
                return new ImageStatusResult(ImageStatus.IN_PROGRESS, percentage);
            }
        } catch (RuntimeException ex) {
            String msg = String.format("Failed to check the status of the image in resource group '%s', image storage name '%s'",
                    imageResourceGroupName, imageStorageName);
            LOGGER.warn(msg, ex);
            return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
        }
    }

    @Override
    public void prerequisites(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);
        CloudResource cloudResource = new Builder().type(ResourceType.ARM_TEMPLATE).name(resourceGroupName).build();
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
    public void createEnvironmentPrerequisites(EnvironmentPrerequisitesCreateRequest environmentPrerequisitesCreateRequest) {
        AzureClient azureClient = azureClientService.getClient(environmentPrerequisitesCreateRequest.getCloudCredential());
        azureClient.createResourceGroup(
                environmentPrerequisitesCreateRequest.getAzure().getResourceGroupName(),
                environmentPrerequisitesCreateRequest.getAzure().getLocationName(),
                environmentPrerequisitesCreateRequest.getAzure().getTags());
    }

    @Override
    public void deleteEnvironmentPrerequisites(EnvironmentPrerequisiteDeleteRequest environmentPrerequisiteDeleteRequest) {
        AzureClient azureClient = azureClientService.getClient(environmentPrerequisiteDeleteRequest.getCloudCredential());
        environmentPrerequisiteDeleteRequest.getAzurePrerequisiteDeleteRequest()
                .map(AzurePrerequisiteDeleteRequest::getResourceGroupName)
                .ifPresentOrElse(
                        resourceGroupName -> deleteResourceGroup(azureClient, resourceGroupName),
                        () -> LOGGER.debug("No azure resource group was defined, not deleting resource group.")
                );
    }

    private void deleteResourceGroup(AzureClient azureClient, String resourceGroupName) {
        try {
            if (!azureClient.isResourceGroupEmpty(resourceGroupName)) {
                String message = String.format("Resource group %s is not empty, cannot proceed with deleting the environment. " +
                        "Please check contents on azure portal.", resourceGroupName);
                LOGGER.warn(message);
                throw new CloudConnectorException(message);
            }
            azureUtils.deleteResourceGroup(azureClient, resourceGroupName, false);
        } catch (CloudException e) {
            throw new CloudConnectorException("Could not delete resource group", e);
        }
    }

    @Override
    public void validateParameters(AuthenticatedContext ac, Map<String, String> parameters) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = parameters.get(PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER);
        ResourceGroupUsage resourceGroupUsage = getIfNotNull(
                parameters.get(PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER),
                ResourceGroupUsage::valueOf);

        if (StringUtils.isEmpty(resourceGroupName)) {
            return;
        }
        if (ResourceGroupUsage.MULTIPLE.equals(resourceGroupUsage)) {
            LOGGER.debug("Multiple RG mode is active, checking existence of resource group {}", resourceGroupName);
            ResourceGroup resourceGroup = client.getResourceGroup(resourceGroupName);
            if (resourceGroup != null) {
                throw new BadRequestException("Resourcegroup name already exists: " + resourceGroup.name());
            }
        }
    }

    @Override
    public void scalingPrerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, boolean upscale) {

    }

    @Override
    public void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) throws Exception {
        FileSystemType fileSystemType = spiFileSystem.getType();
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        if (cloudFileSystems.size() > 2) {
            throw new CloudConnectorException("More than 2 file systems (identities) are not yet supported on Azure!");
        }
        if (cloudFileSystems.isEmpty()) {
            LOGGER.info("No filesystem was configured.");
            return;
        }
        if (FileSystemType.ADLS.equals(fileSystemType)) {
            validateAdlsFileSystem(credential, spiFileSystem);
        } else if (FileSystemType.ADLS_GEN_2.equals(fileSystemType)) {
            validateAdlsGen2FileSystem(spiFileSystem);
        } else {
            validateWasbFileSystem(spiFileSystem);
        }
    }

    private void validateAdlsFileSystem(CloudCredential credential, SpiFileSystem fileSystem) {
        Map<String, Object> credentialAttributes = credential.getParameters();
        CloudAdlsView cloudFileSystem = (CloudAdlsView) fileSystem.getCloudFileSystems().get(0);
        String clientSecret = String.valueOf(credentialAttributes.get(CloudAdlsView.CREDENTIAL_SECRET_KEY));
        String subscriptionId = String.valueOf(credentialAttributes.get(CloudAdlsView.SUBSCRIPTION_ID));
        String clientId = String.valueOf(credentialAttributes.get(CloudAdlsView.ACCESS_KEY));
        String tenantId = StringUtils.isEmpty(cloudFileSystem.getTenantId()) ? credential.getStringParameter(TENANT_ID) : cloudFileSystem.getTenantId();
        String accountName = cloudFileSystem.getAccountName();

        ApplicationTokenCredentials creds = new ApplicationTokenCredentials(clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
        DataLakeStoreAccountManagementClient adlsClient = new DataLakeStoreAccountManagementClientImpl(creds);
        adlsClient.withSubscriptionId(subscriptionId);
        PagedList<DataLakeStoreAccountBasic> dataLakeStoreAccountPagedList = adlsClient.accounts().list();
        boolean validAccountname = false;

        List<DataLakeStoreAccountBasic> dataLakeStoreAccountList = new ArrayList<>();
        while (dataLakeStoreAccountPagedList.hasNextPage()) {
            dataLakeStoreAccountList.addAll(dataLakeStoreAccountPagedList);
            dataLakeStoreAccountPagedList.loadNextPage();
        }

        for (DataLakeStoreAccountBasic account : dataLakeStoreAccountList) {
            if (account.name().equalsIgnoreCase(accountName)) {
                validAccountname = true;
                break;
            }
        }
        if (!validAccountname) {
            throw new CloudConnectorException("The provided file system account name does not belong to a valid ADLS account");
        }
    }

    private void validateAdlsGen2FileSystem(SpiFileSystem fileSystem) throws URISyntaxException, InvalidKeyException, StorageException {
        CloudAdlsGen2View cloudFileSystem = (CloudAdlsGen2View) fileSystem.getCloudFileSystems().get(0);
        String accountName = cloudFileSystem.getAccountName();
        String accountKey = cloudFileSystem.getAccountKey();
        String connectionString = "DefaultEndpointsProtocol=https;AccountName="
                + accountName + ";AccountKey=" + accountKey + ";EndpointSuffix=core.windows.net";
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer containerReference = blobClient.getContainerReference(TEST_CONTAINER + System.nanoTime());
        try {
            containerReference.createIfNotExists();
            containerReference.delete();
        } catch (StorageException e) {
            if (e.getCause() instanceof UnknownHostException) {
                throw new CloudConnectorException("The provided account does not belong to a valid storage account");
            }
        }
    }

    private void validateWasbFileSystem(SpiFileSystem fileSystem) throws URISyntaxException, InvalidKeyException, StorageException {
        CloudWasbView cloudFileSystem = (CloudWasbView) fileSystem.getCloudFileSystems().get(0);
        String accountName = cloudFileSystem.getAccountName();
        String accountKey = cloudFileSystem.getAccountKey();
        String connectionString = "DefaultEndpointsProtocol=https;AccountName=" + accountName + ";AccountKey=" + accountKey;
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer containerReference = blobClient.getContainerReference(TEST_CONTAINER + System.nanoTime());
        try {
            containerReference.createIfNotExists();
            containerReference.delete();
        } catch (StorageException e) {
            if (e.getCause() instanceof UnknownHostException) {
                throw new CloudConnectorException("The provided account does not belong to a valid storage account");
            }
        }
    }

    private boolean storageContainsImage(AzureClient client, String resourceGroupName, String storageName, String image) {
        List<ListBlobItem> listBlobItems = client.listBlobInStorage(resourceGroupName, storageName, IMAGES_CONTAINER);
        for (ListBlobItem listBlobItem : listBlobItems) {
            if (getNameFromConnectionString(listBlobItem.getUri().getPath()).equals(image.split("/")[image.split("/").length - 1])) {
                return true;
            }
        }
        return false;
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
