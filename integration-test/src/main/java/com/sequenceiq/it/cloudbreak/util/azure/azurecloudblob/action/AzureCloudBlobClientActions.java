package com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.action;

import static com.azure.storage.blob.models.BlobErrorCode.CONTAINER_ALREADY_EXISTS;
import static java.lang.String.format;

import java.net.URI;
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

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import com.azure.storage.file.datalake.models.PathItem;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.client.AzureCloudBlobClient;

@Component
public class AzureCloudBlobClientActions extends AzureCloudBlobClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudBlobClientActions.class);

    @Inject
    private AzureProperties azureProperties;

    /**
     * Get a reference to the DataLakeFileSystemClient object with given name in the related storage account.
     * This object can be used to create the container on the service, list blobs, delete the container, etc.
     * <p>
     * This operation does make a call to the Azure Storage service. It
     * creates the container on the service if it does not exist.
     *
     * @param fileSystemName The name of the created file system in the storage account.
     * @return DataLakeFileSystemClient
     */
    private DataLakeFileSystemClient getDataLakeFileSystemClient(String fileSystemName) {
            DataLakeFileSystemClient fileSystemClient = createDataLakeServiceClient().getFileSystemClient(fileSystemName);
        try {
            fileSystemClient.create();
            return fileSystemClient;
        } catch (DataLakeStorageException error) {
            if (BlobErrorCode.fromString(error.getErrorCode()).equals(CONTAINER_ALREADY_EXISTS)) {
                LOGGER.info("Can't create file system {}. It already exists, ignoring this error.", fileSystemName);
                return fileSystemClient;
            } else {
                throw error;
            }
        }
    }

    private String getContainerName(String baseLocation) {
        String containerName = baseLocation.substring(baseLocation.lastIndexOf("//") + 1, baseLocation.lastIndexOf("@"));
        containerName = containerName.replaceFirst("^\\/", "");
        LOGGER.info("Container Name: {} at path: {}", containerName, baseLocation);
        return containerName;
    }

    private String getContainerName() {
        String fullPath = azureProperties.getCloudStorage().getBaseLocation();
        return getContainerName(fullPath);
    }

    private void validateBlobItemLength(PathItem blobItem, boolean zeroContent, Set<String> blobsWithZeroLength) {
        if (blobItem.getContentLength() == 0L && !zeroContent) {
            String itemName = blobItem.getName();
            blobsWithZeroLength.add(itemName);
            Integer zeroBlobLengthToleration = azureProperties.getCloudStorage().getZeroBlobLengthToleration();
            if (blobsWithZeroLength.size() >= zeroBlobLengthToleration) {
                LOGGER.error("Zero blob length toleration limit ({}) reached! The following blobs has 0 bytes content: {}",
                        zeroBlobLengthToleration, StringUtils.join(blobsWithZeroLength, ", "));
                throw new TestFailException(String.format("Azure Adls Gen 2 Blob: %s has 0 bytes of content!", itemName));
            } else {
                LOGGER.warn(" Azure Adls Gen 2 Blob: {} has 0 bytes of content! (blobs with no content - occurrence: {}, limit: {})",
                        itemName, blobsWithZeroLength.size(), zeroBlobLengthToleration);
            }
        }
    }

    public void createBlobContainerClient() {
        BlobContainerClient cloudBlobContainer;
        String containerName = getContainerName();

        try {
            int limit = 0;
            do {
                try {
                    cloudBlobContainer = createCloudBlobClient(containerName);
                    if (cloudBlobContainer.createIfNotExists()) {
                        Log.log(LOGGER, format(" Container with Name: %s has been created. ", containerName));
                    } else {
                        Log.log(LOGGER, format(" Container with Name: %s already exists. ", containerName));
                    }
                    limit = 60;
                } catch (BlobStorageException createAfterWaitException) {
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
        DataLakeFileSystemClient cloudBlobContainer = getDataLakeFileSystemClient(containerName);

        try {
            Log.log(LOGGER, format(" Removing Azure Adls Gen 2 Blob Container with Name: %s at Base Location: %s", containerName, baseLocation));
            cloudBlobContainer.deleteIfExists();
            Log.log(LOGGER, format(" Azure Adls Gen 2 Blob Container: %s delete has been initiated. ", containerName));
        } catch (DataLakeStorageException e) {
            if (BlobErrorCode.fromString(e.getErrorCode()) == BlobErrorCode.CONTAINER_NOT_FOUND) {
                LOGGER.error("Azure Adls Gen2 Blob Container does not present with name: {} at Base Location: {}", containerName, baseLocation);
                throw new TestFailException("Azure Adls Gen2 Blob Container does not present with name: " +  containerName
                        + " at Base Location: " + baseLocation);
            } else {
                LOGGER.error("Azure Adls Gen2 Blob Container delete cannot succeed!", e);
                throw new TestFailException("Azure Adls Gen2 Blob Container delete cannot succeed!", e);
            }
        }
    }

    public void listSelectedDirectory(String baseLocation, String selectedDirectory, boolean zeroContent) {
        String containerName = getContainerName(baseLocation);
        DataLakeFileSystemClient cloudBlobContainer = getDataLakeFileSystemClient(containerName);
        String keyPrefix = getKeyPrefix(baseLocation);

        Log.log(LOGGER, format(" Azure Blob Storage URI: %s", cloudBlobContainer.getFileSystemUrl()));
        Log.log(LOGGER, format(" Azure Blob Container: %s", cloudBlobContainer.getFileSystemName()));
        Log.log(LOGGER, format(" Azure Blob Key Prefix: %s", keyPrefix));
        Log.log(LOGGER, format(" Azure Blob Directory: %s", selectedDirectory));

        try {
            DataLakeDirectoryClient selectedStorageDirectory = cloudBlobContainer.getDirectoryClient(keyPrefix);
            DataLakeDirectoryClient selectedBlobDirectory = selectedStorageDirectory.getSubdirectoryClient(selectedDirectory);
            Set<String> blobsWithZeroLength = new HashSet<>();

            PagedIterable<PathItem> blobListing = selectedBlobDirectory.listPaths(true, false, null, null);
            List<PathItem> listBlobItems = StreamSupport
                    .stream(blobListing.spliterator(), false)
                    .collect(Collectors.toList());
            Log.log(LOGGER, format(" Azure Blob: %s contains %d sub-objects or present with occurrences.",
                    selectedDirectory, listBlobItems.size()));

            for (PathItem blob : blobListing) {
                if (!blob.isDirectory()) {
                    validateBlobItemLength(blob, zeroContent, blobsWithZeroLength);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Azure Adls Gen 2 Blob couldn't process the call. So returning with error!", e);
            throw new TestFailException("Azure Adls Gen 2 Blob couldn't process the call.", e);
        }
    }

    public URI getBaseLocationUri(String baseLocation) {
        try {
            return new URI(baseLocation);
        } catch (Exception e) {
            LOGGER.error("Azure Adls base location path: '{}' is not a valid URI!", baseLocation);
            throw new TestFailException(format(" Azure Adls base location path: '%s' is not a valid URI! ", baseLocation));
        }
    }

    public String getKeyPrefix(String baseLocation) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        return StringUtils.removeStart(baseLocationUri.getPath(), "/");
    }

    public String getLoggingUrl(String baseLocation, String clusterLogPath) {
        if (StringUtils.isNotBlank((baseLocation))) {
            String containerName = getContainerName(baseLocation);
            DataLakeFileSystemClient cloudBlobContainer = getDataLakeFileSystemClient(containerName);
            String keyPrefix = getKeyPrefix(baseLocation);

            Log.log(LOGGER, format(" Azure Blob Storage URI: %s", cloudBlobContainer.getFileSystemUrl()));
            Log.log(LOGGER, format(" Azure Blob Container: %s", cloudBlobContainer.getFileSystemName()));
            Log.log(LOGGER, format(" Azure Blob Log Path: %s", keyPrefix));
            Log.log(LOGGER, format(" Azure Blob Cluster Logs: %s", clusterLogPath));

            if (StringUtils.contains(keyPrefix, clusterLogPath)) {
                LOGGER.info("Azure Adls Gen 2 Blob is present at '{}' container in storage directory with path: [{}]", containerName, keyPrefix);
                return format("https://autotestingapi.blob.core.windows.net/%s/%s",
                        containerName, keyPrefix);
            }
            try {
                DataLakeDirectoryClient storageDirectory = cloudBlobContainer.getDirectoryClient(keyPrefix);
                DataLakeDirectoryClient logsDirectory = storageDirectory.getSubdirectoryClient(clusterLogPath);
                if (logsDirectory.listPaths().iterator().hasNext()) {
                    return format("https://autotestingapi.blob.core.windows.net/%s/%s%s",
                            containerName, keyPrefix, clusterLogPath);
                } else {
                    LOGGER.error("Azure Adls Gen 2 Blob is NOT present at '{}' container in '{}' storage directory with path: [{}]", containerName, keyPrefix,
                            clusterLogPath);
                    throw new TestFailException(format("Azure Adls Gen 2 Blob is NOT present at '%s' container in '%s' storage directory with path: [%s]",
                            containerName, keyPrefix, clusterLogPath));
                }
            } catch (TestFailException testFail) {
                throw testFail;
            } catch (Exception e) {
                LOGGER.error("Azure Adls Gen 2 Blob couldn't process the call. So returning with error!", e);
                throw new TestFailException("Azure Adls Gen 2 Blob couldn't process the call.", e);
            }
        } else {
            return null;
        }
    }
}