package com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.action;

import static java.lang.String.format;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
            throw new TestFailException("The Storage Container is not exist: [" + containerName + "]", e);
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

    private String getContainerName(String baseLocation) {
        final String containerName = baseLocation.substring(baseLocation.lastIndexOf("/") + 1, baseLocation.lastIndexOf("@"));
        LOGGER.info("Container Name: {} at Base Location: {}", containerName, baseLocation);
        return containerName;
    }

    private String getContainerName() {
        String fullPath = azureProperties.getCloudStorage().getBaseLocation();
        final String containerName = fullPath.substring(fullPath.lastIndexOf("/") + 1, fullPath.lastIndexOf("@"));
        LOGGER.info("Container Name: {} at Path: {}", containerName, fullPath);
        return containerName;
    }

    private void deleteBlobsInDirectory(CloudBlobContainer cloudBlobContainer, String directoryName)
            throws URISyntaxException, StorageException {

        CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(directoryName);
        for (ListBlobItem blobItem : blobDirectory.listBlobs()) {
            if (blobItem instanceof CloudBlobDirectory) {
                deleteBlobsInDirectory(cloudBlobContainer, ((CloudBlobDirectory) blobItem).getPrefix());
            } else if (blobItem instanceof CloudPageBlob) {
                CloudPageBlob cloudPageBlob = cloudBlobContainer.getPageBlobReference(((CloudPageBlob) blobItem).getName());
                cloudPageBlob.deleteIfExists();
            } else if (blobItem instanceof CloudBlockBlob) {
                CloudBlockBlob cloudBlockBlob = cloudBlobContainer.getBlockBlobReference(((CloudBlockBlob) blobItem).getName());
                cloudBlockBlob.deleteIfExists();
            }
        }
    }

    private void listBlobsInDirectory(CloudBlobContainer cloudBlobContainer, String directoryName)
            throws URISyntaxException, StorageException {

        CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(directoryName);

        for (ListBlobItem blobItem : blobDirectory.listBlobs()) {
            if (blobItem instanceof CloudBlobDirectory) {
                listBlobsInDirectory(cloudBlobContainer, ((CloudBlobDirectory) blobItem).getPrefix());
            } else if (blobItem instanceof CloudPageBlob) {
                Log.log(LOGGER, format(" Azure Adls Gen 2 Cloud Page Blob is present with Name: [%s] and with bytes of content: [%d] at URI: [%s] ",
                        ((CloudPageBlob) blobItem).getName(), ((CloudPageBlob) blobItem).getProperties().getLength(), blobItem.getUri().getPath()));
            } else if (blobItem instanceof CloudBlockBlob) {
                Log.log(LOGGER, format(" Azure Adls Gen 2 Cloud Block Blob is present with Name: [%s] and with bytes of content: [%d] at URI: [%s] ",
                        ((CloudBlockBlob) blobItem).getName(), ((CloudBlockBlob) blobItem).getProperties().getLength(), blobItem.getUri().getPath()));
            } else {
                LOGGER.error("Azure Adls Gen 2 Cloud Storage Item that is present at URI: [{}] cannot be classify as CloudBlob, CloudPageBlob and " +
                        "CloudBlockBlob. ", blobItem.getUri().getPath());
                throw new TestFailException(String.format("Azure Adls Gen 2 Cloud Storage Item that is present at URI: [%s] cannot be classify as" +
                        " CloudBlob, CloudPageBlob and CloudBlockBlob. ", blobItem.getUri().getPath()));
            }
        }
    }

    private void listBlobsInDirectoryWithValidation(CloudBlobContainer cloudBlobContainer, String directoryName, Boolean zeroContent)
            throws URISyntaxException, StorageException {

        CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(directoryName);
        Set<String> blobsWithZeroLength = new HashSet<>();

        for (ListBlobItem blobItem : blobDirectory.listBlobs()) {
            if (blobItem instanceof CloudBlobDirectory) {
                listBlobsInDirectoryWithValidation(cloudBlobContainer, ((CloudBlobDirectory) blobItem).getPrefix(), zeroContent);
            } else if (blobItem instanceof CloudPageBlob) {
                validateBlobItemLength(blobItem, zeroContent, blobsWithZeroLength);
            } else if (blobItem instanceof CloudBlockBlob) {
                validateBlobItemLength(blobItem, zeroContent, blobsWithZeroLength);
            } else {
                LOGGER.error("Azure Adls Gen 2 Cloud Storage Item that is present at URI: {} cannot be classify as CloudBlob, CloudPageBlob and " +
                        "CloudBlockBlob. ", blobItem.getUri().getPath());
                throw new TestFailException(String.format("Azure Adls Gen 2 Cloud Storage Item that is present at URI: %s cannot be classify as" +
                        " CloudBlob, CloudPageBlob and CloudBlockBlob. ", blobItem.getUri().getPath()));
            }
        }
    }

    private void validateBlobItemLength(ListBlobItem blobItem, Boolean zeroContent, Set<String> blobsWithZeroLength) {
        if (((CloudBlob) blobItem).getProperties().getLength() == 0 && !zeroContent) {
            blobsWithZeroLength.add(((CloudBlob) blobItem).getName());
            Integer zeroBlobLengthToleration = azureProperties.getCloudStorage().getZeroBlobLengthToleration();
            if (blobsWithZeroLength.size() >= zeroBlobLengthToleration) {
                LOGGER.error("Zero blob length toleration limit ({}) reached! The following blobs has 0 bytes content: {}",
                        zeroBlobLengthToleration, StringUtils.join(blobsWithZeroLength, ", "));
                throw new TestFailException(String.format("Azure Adls Gen 2 Blob: %s has 0 bytes of content!", ((CloudBlob) blobItem).getName()));
            } else {
                LOGGER.warn(" Azure Adls Gen 2 Blob: {} has 0 bytes of content! (blobs with no content - occurrence: {}, limit: {})",
                        ((CloudBlob) blobItem).getName(), blobsWithZeroLength.size(), zeroBlobLengthToleration);
            }
        }
    }

    public void createCloudBlobContainer() {
        CloudBlobContainer cloudBlobContainer;
        String containerName = getContainerName();

        try {
            int limit = 0;
            do {
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
                    if (limit >= 10) {
                        LOGGER.error("Azure Adls Gen2 Blob Storage Container: {} create cannot be succeed during 60000 ms!\n",
                                containerName, createAfterWaitException);
                    }
                    Thread.sleep(6000);
                }
            } while (limit < 60);
        } catch (InterruptedException waitException) {
            LOGGER.error("Creation of Adls Gen2 Blob Storage Container: {} has been timed out after 360000 ms!\n", containerName, waitException);
        }
    }

    public void cleanupContainer(String baseLocation) {
        String containerName = getContainerName(baseLocation);
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        try {
            Log.log(LOGGER, format(" Removing Azure Adls Gen 2 Blob Container with Name: %s at Base Location: %s", containerName, baseLocation));
            cloudBlobContainer.deleteIfExists();
            Log.log(LOGGER, format(" Azure Adls Gen 2 Blob Container: %s delete has been initiated. ", containerName));
        } catch (StorageException e) {
            if (e.getHttpStatusCode() == 404) {
                LOGGER.error("Azure Adls Gen2 Blob Container does not present with name: {} at Base Location: {}", containerName, baseLocation);
                throw new TestFailException("Azure Adls Gen2 Blob Container does not present with name: " +  containerName
                        + " at Base Location: " + baseLocation);
            } else {
                LOGGER.error("Azure Adls Gen2 Blob Container delete cannot be succeed!", e);
                throw new TestFailException("Azure Adls Gen2 Blob Container delete cannot be succeed!", e);
            }
        } finally {
            createCloudBlobContainer(containerName);
        }
    }

    public SdxTestDto deleteAllFolders(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        String containerName = getContainerName(sdxTestDto.getRequest().getCloudStorage().getBaseLocation());
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        try {
            for (ListBlobItem blob : cloudBlobContainer.listBlobs()) {
                String blobName = blob.getUri().getPath().split("/", 3)[2];
                String blobUriPath = blob.getUri().getPath();

                if (blob instanceof CloudBlob) {
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
            throw new TestFailException("Azure Adls Gen 2 Blob couldn't process the call.", e);
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
            throw e;
        }
    }

    public void listAllFolders(String baseLocation) {
        String containerName = getContainerName(baseLocation);
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        Log.log(LOGGER, format(" Azure Blob Storage URI: %s", cloudBlobContainer.getStorageUri()));
        Log.log(LOGGER, format(" Azure Blob Container: %s", cloudBlobContainer.getName()));

        try {
            for (ListBlobItem blob : cloudBlobContainer.listBlobs()) {
                String blobName = blob.getUri().getPath().split("/", 3)[2];
                String blobUriPath = blob.getUri().getPath();

                if (blob instanceof CloudBlob) {
                    if (((CloudBlob) blob).exists()) {
                        Log.log(LOGGER, format(" Azure Adls Gen 2 Blob is present with Name: %s and with bytes of content: %d at URI: %s ",
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
            throw new TestFailException("Azure Adls Gen 2 Blob couldn't process the call.", e);
        }
    }

    public void listSelectedDirectory(String baseLocation, String selectedDirectory, Boolean zeroContent) {
        String containerName = getContainerName(baseLocation);
        CloudBlobContainer cloudBlobContainer = getCloudBlobContainer(containerName);

        Log.log(LOGGER, format(" Azure Blob Storage URI: %s", cloudBlobContainer.getStorageUri()));
        Log.log(LOGGER, format(" Azure Blob Container: %s", cloudBlobContainer.getName()));
        Log.log(LOGGER, format(" Azure Blob Directory: %s", selectedDirectory));

        try {
            CloudBlobDirectory logsDirectory = cloudBlobContainer.getDirectoryReference("cluster-logs");
            CloudBlobDirectory selectedLogsDirectory = logsDirectory.getDirectoryReference(selectedDirectory);
            Set<String> blobsWithZeroLength = new HashSet<>();

            Iterable<ListBlobItem> blobListing = cloudBlobContainer.listBlobs("cluster-logs/" + selectedDirectory, true);
            List<ListBlobItem> listBlobItems = StreamSupport
                    .stream(blobListing.spliterator(), false)
                    .collect(Collectors.toList());
            Log.log(LOGGER, format(" Azure Blob Directory: %s contains %d sub-objects.",
                    selectedDirectory, listBlobItems.size()));

            for (ListBlobItem blob : selectedLogsDirectory.listBlobs()) {
                String blobName = blob.getUri().getPath().split("/", 3)[2];
                String blobUriPath = blob.getUri().getPath();

                if (blob instanceof CloudBlob) {
                    if (((CloudBlob) blob).exists()) {
                        validateBlobItemLength(blob, zeroContent, blobsWithZeroLength);
                    } else {
                        LOGGER.error("Azure Adls Gen 2 Blob is NOT present with Name: {} and with bytes of content: {} at URI: {}",
                                ((CloudBlob) blob).getName(), ((CloudBlob) blob).getProperties().getLength(), blobUriPath);
                        throw new TestFailException(String.format("Azure Adls Gen 2 Blob is NOT present with Name: %s", ((CloudBlob) blob).getName()));
                    }
                } else {
                    if (blobName.endsWith("/")) {
                        blobName = blobName.replaceAll(".$", "");
                    }
                    CloudBlobDirectory blobDirectory = cloudBlobContainer.getDirectoryReference(blobName);
                    listBlobsInDirectoryWithValidation(cloudBlobContainer, blobDirectory.getPrefix(), zeroContent);
                }
            }
        } catch (StorageException | URISyntaxException e) {
            LOGGER.error("Azure Adls Gen 2 Blob couldn't process the call. So it has been returned with error!", e);
            throw new TestFailException("Azure Adls Gen 2 Blob couldn't process the call.", e);
        }
    }
}
