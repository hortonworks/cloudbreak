package com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;

@Service
public class AzureCloudBlobClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudBlobClient.class);

    @Inject
    private AzureProperties azureProperties;

    public AzureCloudBlobClient() {
    }

    /**
     * Create a BlobContainerClient object from the storage account.
     * This object is the root object for all operations on the
     * blob service for this particular account.
     *
     * @return BlobContainerClient
     */
    public BlobContainerClient createCloudBlobClient(String containerName) {
        String accountName = azureProperties.getCloudStorage().getAccountName();
        String accountKey = azureProperties.getCloudStorage().getAccountKey();
        BlobContainerClient blobContainerClient;
        try {
            blobContainerClient = new BlobContainerClientBuilder()
                    .endpoint("https://" + accountName + ".blob.core.windows.net")
                    .containerName(containerName)
                    .credential(new StorageSharedKeyCredential(accountName, accountKey))
                    .buildClient();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Please confirm the connection string is in the Azure connection string format.");
            LOGGER.error("\nConnection string specifies an invalid URI.", e);
            throw e;
        } catch (BlobStorageException e) {
            LOGGER.warn("Please confirm the AccountName and AccountKey in the connection string are valid.");
            throw e;
        }

        return blobContainerClient;
    }

    public DataLakeServiceClient createDataLakeServiceClient() {
        String accountName = azureProperties.getCloudStorage().getAccountName();
        String accountKey = azureProperties.getCloudStorage().getAccountKey();
        try {
            return new DataLakeServiceClientBuilder()
                    .endpoint("https://" + accountName + ".dfs.core.windows.net")
                    .credential(new StorageSharedKeyCredential(accountName, accountKey))
                    .buildClient();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Please confirm the connection string is in the Azure connection string format.");
            LOGGER.error("\nConnection string specifies an invalid URI.", e);
            throw e;
        }
    }
}
