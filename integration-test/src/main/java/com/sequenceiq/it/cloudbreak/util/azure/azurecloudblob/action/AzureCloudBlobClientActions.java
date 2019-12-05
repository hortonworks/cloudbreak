package com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.action;

import static java.lang.String.format;

import java.net.URISyntaxException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.client.AzureCloudBlobClient;

@Component
public class AzureCloudBlobClientActions extends AzureCloudBlobClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudBlobClientActions.class);

    @Inject
    private AzureProperties azureProperties;

    /**
     * Get a reference to the CloudBlobContainer object with given name in the related storage account.
     * This object can be used to create the container on the service, list blobs, delete the container, etc.
     *
     * This operation does make a call to the Azure Storage service. It
     * creates the container on the service if it does not exists.
     *
     * @param containerName The name of the created file system in the storage account.
     * @return              CloudBlobContainer
     */
    private CloudBlobContainer getCloudBlobContainer(String containerName) {
        CloudBlobContainer cloudBlobContainer = null;

        try {
            cloudBlobContainer = createCloudBlobClient().getContainerReference(containerName);
        } catch (URISyntaxException | StorageException e) {
            LOGGER.error("The Storage Container is not exist: [{}]\n", containerName, e);
            throw new TestFailException("The Storage Container is not exist: [" + containerName + "]\n" + e);
        }

        return cloudBlobContainer;
    }

    private void createCloudBlobContainer(String containerName) {
        CloudBlobContainer cloudBlobContainer = null;

        try {
            int limit = 0;
            do {
                Thread.sleep(6000);
                try {
                    cloudBlobContainer = createCloudBlobClient().getContainerReference(containerName);
                    if (cloudBlobContainer.createIfNotExists()) {
                        Log.log(LOGGER, format(" Container with Name: %s has been created. ", containerName));
                    } else {
                        Log.log(LOGGER, format(" Container with Name: %s already exists. ", containerName));
                    }
                    limit = 60;
                } catch (StorageException | URISyntaxException createAfterWaitException) {
                    limit++;
                    if (limit >= 60) {
                        LOGGER.error("Azure Adls Gen2 Blob Storage Container [{}] create cannot be succeed during 360000 ms!\n",
                                containerName, createAfterWaitException);
                    }
                }
            } while (limit < 60);
        } catch (InterruptedException waitException) {
            LOGGER.error("Creation of Adls Gen2 Blob Storage Container [{}] has been timed out after 360000 ms!\n", containerName, waitException);
        }
    }

    private String getContainerName(SdxTestDto sdxTestDto) {
        String fullPath = sdxTestDto.getRequest().getCloudStorage().getBaseLocation();
        final String containerName = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("@"));
        Log.log(LOGGER, format(" Container Name: [%s] ", containerName));
        return containerName;
    }

    private String getContainerName() {
        String fullPath = azureProperties.getCloudstorage().getBaseLocation();
        final String containerName = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("@"));
        Log.log(LOGGER, format(" Container Name: [%s] ", containerName));
        return containerName;
    }

    private void deleteBlobsInDirectory(CloudBlobContainer cloudBlobContainer, String directoryName)
            throws URISyntaxException, StorageException {

        CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(directoryName);
        for (ListBlobItem blobItem : blobDirectory.listBlobs()) {
            if (blobItem instanceof CloudBlobDirectory) {
                Log.log(LOGGER, format(" Azure Adls Gen 2 Blob Directory is present with Prefix: [%s] at URI: [%s] ",
                        ((CloudBlobDirectory) blobItem).getPrefix(), blobItem.getUri().getPath()));
                deleteBlobsInDirectory(cloudBlobContainer, ((CloudBlobDirectory) blobItem).getPrefix());
            } else if (blobItem instanceof CloudPageBlob) {
                CloudPageBlob cloudPageBlob = cloudBlobContainer.getPageBlobReference(((CloudPageBlob) blobItem).getName());
                Log.log(LOGGER, format(" Deleting Azure Adls Gen 2 Cloud Page Blob with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                        ((CloudPageBlob) blobItem).getName(), ((CloudPageBlob) blobItem).getProperties().getLength(), blobItem.getUri().getPath()));
                cloudPageBlob.deleteIfExists();
            } else if (blobItem instanceof CloudBlockBlob) {
                CloudBlockBlob cloudBlockBlob = cloudBlobContainer.getBlockBlobReference(((CloudBlockBlob) blobItem).getName());
                Log.log(LOGGER, format(" Deleting Azure Adls Gen 2 Cloud Block Blob with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                        ((CloudBlockBlob) blobItem).getName(), ((CloudBlockBlob) blobItem).getProperties().getLength(), blobItem.getUri().getPath()));
                cloudBlockBlob.deleteIfExists();
            }
        }
    }

    private void listBlobsInDirectory(CloudBlobContainer cloudBlobContainer, String directoryName)
            throws URISyntaxException, StorageException {

        CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(directoryName);

        for (ListBlobItem blobItem : blobDirectory.listBlobs()) {
            if (blobItem instanceof CloudBlobDirectory) {
                Log.log(LOGGER, format(" Azure Adls Gen 2 Cloud Blob Directory is present with Prefix: [%s] at URI: [%s] ",
                        ((CloudBlobDirectory) blobItem).getPrefix(), blobItem.getUri().getPath()));
                listBlobsInDirectory(cloudBlobContainer, ((CloudBlobDirectory) blobItem).getPrefix());
            } else if (blobItem instanceof CloudPageBlob) {
                Log.log(LOGGER, format(" Azure Adls Gen 2 Cloud Page Blob is present with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                        ((CloudPageBlob) blobItem).getName(), ((CloudPageBlob) blobItem).getProperties().getLength(), blobItem.getUri().getPath()));
            } else if (blobItem instanceof CloudBlockBlob) {
                Log.log(LOGGER, format(" Azure Adls Gen 2 Cloud Block Blob is present with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                        ((CloudBlockBlob) blobItem).getName(), ((CloudBlockBlob) blobItem).getProperties().getLength(), blobItem.getUri().getPath()));
            } else {
                LOGGER.error("Azure Adls Gen 2 Cloud Storage Item that is present at URI: [{}] cannot be classify as CloudBlob, CloudPageBlob and " +
                        "CloudBlockBlob. ", blobItem.getUri().getPath());
                throw new TestFailException(String.format("Azure Adls Gen 2 Cloud Storage Item that is present at URI: [%s] cannot be classify as" +
                        " CloudBlob, CloudPageBlob and CloudBlockBlob. ", blobItem.getUri().getPath()));
            }
        }
    }

    public void createCloudBlobContainer() {
        CloudBlobContainer cloudBlobContainer = null;
        String containerName = getContainerName();

        try {
            int limit = 0;
            do {
                Thread.sleep(6000);
                try {
                    cloudBlobContainer = createCloudBlobClient().getContainerReference(containerName);
                    if (cloudBlobContainer.createIfNotExists()) {
                        Log.log(LOGGER, format(" Container with Name: %s has been created. ", containerName));
                    } else {
                        Log.log(LOGGER, format(" Container with Name: %s already exists. ", containerName));
                    }
                    limit = 60;
                } catch (StorageException | URISyntaxException createAfterWaitException) {
                    limit++;
                    if (limit >= 60) {
                        LOGGER.error("Azure Adls Gen2 Blob Storage Container [{}] create cannot be succeed during 360000 ms!\n",
                                containerName, createAfterWaitException);
                    }
                }
            } while (limit < 60);
        } catch (InterruptedException waitException) {
            LOGGER.error("Creation of Adls Gen2 Blob Storage Container [{}] has been timed out after 360000 ms!\n", containerName, waitException);
        }
    }

    public SdxTestDto cleanupContainer(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        String containerName = getContainerName(sdxTestDto);
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        try {
            Log.log(LOGGER, format(" Removing Azure Adls Gen 2 Blob Container with Name: [%s] ", containerName));
            cloudBlobContainer.deleteIfExists();
            Log.log(LOGGER, format(" Azure Adls Gen 2 Blob Container [%s] delete has been initiated. ", containerName));
        } catch (StorageException e) {
            if (e.getHttpStatusCode() == 404) {
                LOGGER.error("Azure Adls Gen2 Blob Storage does not present with name: {}", containerName);
                throw new TestFailException("Azure Adls Gen2 Blob Storage does not present with name: " + containerName);
            } else {
                LOGGER.error("Azure Adls Gen2 Blob Storage delete cannot be succeed!", e);
                throw new TestFailException("Azure Adls Gen2 Blob Storage delete cannot be succeed!" + e);
            }
        } finally {
            createCloudBlobContainer(containerName);
        }

        return sdxTestDto;
    }

    public SdxTestDto deleteAllFolders(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        String containerName = getContainerName(sdxTestDto);
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        try {
            for (ListBlobItem blob : cloudBlobContainer.listBlobs()) {
                String blobName = blob.getUri().getPath().split("/", 3)[2];
                String blobUriPath = blob.getUri().getPath();

                if (blob instanceof CloudBlob) {
                    Log.log(LOGGER, format(" Removing Azure Adls Gen 2 Blob with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                            ((CloudBlob) blob).getName(), ((CloudBlob) blob).getProperties().getLength(), blobUriPath));
                    ((CloudBlob) blob).deleteIfExists();
                } else {
                    if (blobName.endsWith("/")) {
                        blobName = blobName.replaceAll(".$", "");
                    }
                    CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(blobName);
                    deleteBlobsInDirectory(cloudBlobContainer, blobDirectory.getPrefix());
                }
            }
        } catch (StorageException | URISyntaxException e) {
            LOGGER.error("Azure Adls Gen 2 Blob couldn't process the call. So it has been returned with error!", e);
            throw new TestFailException(String.format("Azure Adls Gen 2 Blob couldn't process the call. So it has been returned the error: %s", e));
        }

        return sdxTestDto;
    }

    public void deleteAllFolders() throws StorageException, URISyntaxException {
        String containerName = getContainerName();
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        try {
            for (ListBlobItem blob : cloudBlobContainer.listBlobs()) {
                String blobName = blob.getUri().getPath().split("/", 3)[2];
                String blobUriPath = blob.getUri().getPath();

                if (blob instanceof CloudBlob) {
                    Log.log(LOGGER, format(" Removing Azure Adls Gen 2 Blob with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                            ((CloudBlob) blob).getName(), ((CloudBlob) blob).getProperties().getLength(), blobUriPath));
                    ((CloudBlob) blob).deleteIfExists();
                } else {
                    if (blobName.endsWith("/")) {
                        blobName = blobName.replaceAll(".$", "");
                    }
                    CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(blobName);
                    deleteBlobsInDirectory(cloudBlobContainer, blobDirectory.getPrefix());
                }
            }
        } catch (StorageException | URISyntaxException e) {
            LOGGER.error("\nAzure Adls Gen 2 Blob couldn't process the call. So it has been returned with error!\n", e);
            throw e;
        }
    }

    public SdxTestDto listAllFolders(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        String containerName = getContainerName(sdxTestDto);
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        try {
            for (ListBlobItem blob : cloudBlobContainer.listBlobs()) {
                String blobName = blob.getUri().getPath().split("/", 3)[2];
                String blobUriPath = blob.getUri().getPath();

                if (blob instanceof CloudBlob) {
                    if (((CloudBlob) blob).exists()) {
                        Log.log(LOGGER, format(" Azure Adls Gen 2 Blob is present with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                                ((CloudBlob) blob).getName(), ((CloudBlob) blob).getProperties().getLength(), blobUriPath));
                    } else {
                        Log.log(LOGGER, format(" Azure Adls Gen 2 Blob is NOT present with Name: [%s] and with bites of content: [%d] at URI: [%s] ",
                                ((CloudBlob) blob).getName(), ((CloudBlob) blob).getProperties().getLength(), blobUriPath));
                    }
                } else {
                    if (blobName.endsWith("/")) {
                        blobName = blobName.replaceAll(".$", "");
                    }
                    CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(blobName);
                    listBlobsInDirectory(cloudBlobContainer, blobDirectory.getPrefix());
                }
            }
        } catch (StorageException | URISyntaxException e) {
            LOGGER.error("Azure Adls Gen 2 Blob couldn't process the call. So it has been returned with error!", e);
            throw new TestFailException(String.format("Azure Adls Gen 2 Blob couldn't process the call. So it has been returned the error: %s", e));
        }

        return sdxTestDto;
    }
}
