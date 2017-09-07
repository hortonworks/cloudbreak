package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.datalake.store.DataLakeStoreAccountManagementClient;
import com.microsoft.azure.management.datalake.store.implementation.DataLakeStoreAccountManagementClientImpl;
import com.microsoft.azure.management.datalake.store.models.DataLakeStoreAccount;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class AzureSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSetup.class);

    private static final String TEST_CONTAINER = "cb-test-container";

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage armStorage;

    @Override
    public void prepareImage(AuthenticatedContext ac, CloudStack stack, Image image) {
        LOGGER.info("prepare image: {}", image);

        String imageResourceGroupName = armStorage.getImageResourceGroupName(ac.getCloudContext(), stack.getParameters());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AzureClient client = ac.getParameter(AzureClient.class);
        try {
            copyVhdImageIfNecessary(ac, stack, image, imageResourceGroupName, region, client);
        } catch (Exception ex) {
            LOGGER.error("Could not create image with the specified parameters: {}", ex);
            throw new CloudConnectorException("Image creation failed because " + image.getImageName() + "does not exist or Cloudbreak could not reach.");
        }
        LOGGER.debug("prepare image has been executed");
    }

    private void copyVhdImageIfNecessary(AuthenticatedContext ac, CloudStack stack, Image image, String imageResourceGroupName,
            String region, AzureClient client) throws Exception {
        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), armStorage.getPersistentStorageName(stack.getParameters()),
                armStorage.getArmAttachedStorageOption(stack.getParameters()));
        String resourceGroupName = azureUtils.getResourceGroupName(ac.getCloudContext());
        if (!client.resourceGroupExists(resourceGroupName)) {
            client.createResourceGroup(resourceGroupName, region);
        }
        if (!client.resourceGroupExists(imageResourceGroupName)) {
            client.createResourceGroup(imageResourceGroupName, region);
        }
        armStorage.createStorage(client, imageStorageName, AzureDiskType.LOCALLY_REDUNDANT, imageResourceGroupName, region);
        client.createContainerInStorage(imageResourceGroupName, imageStorageName, IMAGES);
        if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, image.getImageName())) {
            client.copyImageBlobInStorageContainer(imageResourceGroupName, imageStorageName, IMAGES, image.getImageName());
        }
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext ac, CloudStack stack, Image image) {
        String imageResourceGroupName = armStorage.getImageResourceGroupName(ac.getCloudContext(), stack.getParameters());
        AzureClient client = ac.getParameter(AzureClient.class);

        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = armStorage.getImageStorageName(acv, ac.getCloudContext(), armStorage.getPersistentStorageName(stack.getParameters()),
                armStorage.getArmAttachedStorageOption(stack.getParameters()));
        try {
            CopyState copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES, image.getImageName());
            if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
                if (AzureUtils.hasManagedDisk(stack)) {
                    String customImageId = armStorage.getCustomImageId(client, ac, stack);
                    if (customImageId == null) {
                        return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED);
                    }
                }
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            } else if (CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus())) {
                return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
            } else {
                int percentage = (int) (((double) copyState.getBytesCopied() * ImageStatusResult.COMPLETED) / copyState.getTotalBytes());
                LOGGER.info(String.format("CopyStatus Pending %s byte/%s byte: %.4s %%", copyState.getTotalBytes(), copyState.getBytesCopied(), percentage));
                return new ImageStatusResult(ImageStatus.IN_PROGRESS, percentage);
            }
        } catch (RuntimeException ex) {
            return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
        }
    }

    @Override
    public void prerequisites(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        String storageGroup = azureUtils.getResourceGroupName(ac.getCloudContext());
        CloudResource cloudResource = new Builder().type(ResourceType.ARM_TEMPLATE).name(storageGroup).build();
        String region = ac.getCloudContext().getLocation().getRegion().value();
        try {
            AzureClient client = ac.getParameter(AzureClient.class);
            persistenceNotifier.notifyAllocation(cloudResource, ac.getCloudContext());
            if (!client.resourceGroupExists(storageGroup)) {
                client.createResourceGroup(storageGroup, region);
            }
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateFileSystem(CloudCredential credential, FileSystem fileSystem) throws Exception {
        String fileSystemType = fileSystem.getType();
        if (fileSystemType.equalsIgnoreCase(FileSystemType.ADLS.name())) {
            validateAdlsFileSystem(credential, fileSystem);
        } else {
            validateWasbFileSystem(fileSystem, fileSystemType);
        }
    }

    private void validateWasbFileSystem(FileSystem fileSystem, String fileSystemType) throws URISyntaxException, InvalidKeyException, StorageException {
        String accountName = fileSystem.getParameter(WasbFileSystemConfiguration.ACCOUNT_NAME, String.class);
        String accountKey = fileSystem.getParameter(WasbFileSystemConfiguration.ACCOUNT_KEY, String.class);
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

    private void validateAdlsFileSystem(CloudCredential credential, FileSystem fileSystem) {

        Map<String, Object> credentialAttributes = credential.getParameters();
        String clientSecret = String.valueOf(credentialAttributes.get(AdlsFileSystemConfiguration.CREDENTIAL_SECRET_KEY));
        String subscriptionId = String.valueOf(credentialAttributes.get(AdlsFileSystemConfiguration.SUBSCRIPTION_ID));
        String clientId =  String.valueOf(credentialAttributes.get(AdlsFileSystemConfiguration.ACCESS_KEY));
        String tenantId = fileSystem.getStringParameter(AdlsFileSystemConfiguration.TENANT_ID);
        String accountName = fileSystem.getStringParameter(FileSystemConfiguration.ACCOUNT_NAME);

        ApplicationTokenCredentials creds = new ApplicationTokenCredentials(clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
        DataLakeStoreAccountManagementClient adlsClient = new DataLakeStoreAccountManagementClientImpl(creds);
        adlsClient.withSubscriptionId(subscriptionId);
        List<DataLakeStoreAccount> dataLakeStoreAccounts = adlsClient.accounts().list();
        boolean validAccountname = false;

        for (DataLakeStoreAccount account : dataLakeStoreAccounts) {
            if (account.name().equalsIgnoreCase(accountName)) {
                validAccountname = true;
                break;
            }
        }
        if (!validAccountname) {
            throw new CloudConnectorException("The provided file system account name does not belong to a valid ADLS account");
        }
    }

    private boolean storageContainsImage(AzureClient client, String groupName, String storageName, String image) {
        List<ListBlobItem> listBlobItems = client.listBlobInStorage(groupName, storageName, IMAGES);
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
