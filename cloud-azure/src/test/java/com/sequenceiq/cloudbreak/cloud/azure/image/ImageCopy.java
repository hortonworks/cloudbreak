package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;

import org.junit.Test;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.rest.LogLevel;

import okhttp3.JavaNetAuthenticator;

public class ImageCopy {

    private static final int NUMBER_OF_PARALLEL_COPIES = 1;

    public static final long NO_OFFSET = 0L;
    private AzureTestCredentials azureTestCredentials = new AzureTestCredentials();

    @Test
    public void copyImage() throws URISyntaxException, InvalidKeyException, StorageException {
        ImageNameCombinator imageNameCombinator = new ImageNameCombinator(true, "originalCopy", NUMBER_OF_PARALLEL_COPIES);

        Azure azure = Azure
                .configure()
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(LogLevel.BODY_AND_HEADERS)
                .authenticate(azureTestCredentials.getCredentials())
                .withSubscription("3ddda1c7-d1f5-4e7b-ac81-0523f483b3b3");

        String destResourceGroup = "rg-gpapp-single-rg";
        String destStorageName = "cbimgwu9d62091440e606d4";
        String destContainerName = "images";

        for (int i = 0; i < NUMBER_OF_PARALLEL_COPIES; i++) {
            String sourceBlobUri = imageNameCombinator.getSource(i);
            String destinationFileName = imageNameCombinator.getDestinationFilename(i);
            copyImage(azure, destResourceGroup, destStorageName, destContainerName, sourceBlobUri, destinationFileName);
            System.out.println(sourceBlobUri + ", " + destinationFileName);
        }
    }

    private void copyImage(Azure azure, String resourceGroup, String storageName, String containerName, String sourceBlob, String destinationFileName) throws URISyntaxException, InvalidKeyException, StorageException {
        List<StorageAccountKey> keys = getStorageAccountKeys(resourceGroup, storageName, azure);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageName, keys.get(0).value());

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        setDefaultPageBlobOptions(blobClient);
        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        CloudPageBlob cloudPageBlob = container.getPageBlobReference(destinationFileName);
//        String copyId = cloudPageBlob.startCopy(new URI(sourceBlob));

        String copyId = copyWithPageBlobOptions(sourceBlob, cloudPageBlob);
        System.out.println(copyId);
    }

    /*
    Setting of BlobRequestOptions is not in production code, we just experimented with it to parametrize image copy in our trials.
    - setConcurrentRequestCount: does not seem to have any effect on copy operation.
     */
    private void setDefaultPageBlobOptions(CloudBlobClient blobClient) {
        // Set PageBlobOptions, this might set the default
        BlobRequestOptions blobRequestOptions = blobClient.getDefaultRequestOptions();
        blobRequestOptions.setConcurrentRequestCount(10);
        blobRequestOptions.setSingleBlobPutThresholdInBytes(4194303);
    }

    private String copyWithPageBlobOptions(String sourceBlob, CloudPageBlob cloudPageBlob) throws StorageException, URISyntaxException {
        // BlobRequestOptions are not used in production code, we just experimented with it to parametrize image copy in our trials.
        // Set PageBlobOption for one copy, leaves default intact
        BlobRequestOptions blobRequestOptions = new BlobRequestOptions();
        blobRequestOptions.setConcurrentRequestCount(10);
        String copyId = cloudPageBlob.startCopy(new URI(sourceBlob), null, null, blobRequestOptions, null);
        return copyId;
    }

    public List<StorageAccountKey> getStorageAccountKeys(String resourceGroup, String storageName, Azure azure) {
        return getStorageAccountByGroup(resourceGroup, storageName, azure).getKeys();
    }

    public StorageAccount getStorageAccountByGroup(String resourceGroup, String storageName, Azure azure) {
        return azure.storageAccounts().getByResourceGroup(resourceGroup, storageName);
    }

    private final class ImageCopyTask implements Runnable {

        private Azure azure;

        private String resourceGroup;

        private String storageName;

        private String containerName;

        private String sourceBlob;

        public ImageCopyTask(Azure azure, String resourceGroup, String storageName, String containerName, String sourceBlob) {
            this.azure = azure;
            this.resourceGroup = resourceGroup;
            this.storageName = storageName;
            this.containerName = containerName;
            this.sourceBlob = sourceBlob;
        }

        @Override
        public void run() {
            try {
                List<StorageAccountKey> keys = getStorageAccountKeys(resourceGroup, storageName, azure);
                String storageConnectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageName, keys.get(0).value());

                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
                setDefaultPageBlobOptions(blobClient);
                CloudBlobContainer container = blobClient.getContainerReference(containerName);

                CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf('/') + 1));
                String copyId = cloudPageBlob.startCopy(new URI(sourceBlob));

//                String copyId = copyWithPageBlobOptions(sourceBlob, cloudPageBlob);
                System.out.println(copyId);
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

        }
    }
}