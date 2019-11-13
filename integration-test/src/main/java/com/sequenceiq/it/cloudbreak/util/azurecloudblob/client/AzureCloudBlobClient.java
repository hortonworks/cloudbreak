package com.sequenceiq.it.cloudbreak.util.azurecloudblob.client;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;

@Service
public class AzureCloudBlobClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudBlobClient.class);

    @Inject
    private AzureProperties azureProperties;

    public AzureCloudBlobClient() {
    }

    /**
     * Create a CloudStorageAccount object using account name and key.
     * The account name should be just the name of a Storage Account, not a URI, and
     * not including the suffix. The key should be a base-64 encoded string that you
     * can acquire from the portal, or from the management plane.
     * This will have full permissions to all operations on the account.
     *
     * @return CloudStorageAccount
     */
    private CloudStorageAccount getCloudStorageAccount() {
        String accountName = azureProperties.getCloudstorage().getAccountName();
        String accountKey = azureProperties.getCloudstorage().getAccountKey();
        String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=" + accountName + ";AccountKey=" + accountKey;
        CloudStorageAccount storageAccount = null;

        try {
            storageAccount = CloudStorageAccount.parse(storageConnectionString);
        } catch (IllegalArgumentException | URISyntaxException e) {
            LOGGER.warn("Please confirm the connection string is in the Azure connection string format.");
            LOGGER.error("\nConnection string specifies an invalid URI.", e);
        } catch (InvalidKeyException e) {
            LOGGER.warn("Please confirm the AccountName and AccountKey in the connection string are valid.");
            LOGGER.error("\nConnection string specifies an invalid key.", e);
        }

        return storageAccount;
    }

    /**
     * Create a CloudBlobClient object from the storage account.
     * This object is the root object for all operations on the
     * blob service for this particular account.
     *
     * @return CloudBlobClient
     */
    public CloudBlobClient createCloudBlobClient() {
        return getCloudStorageAccount().createCloudBlobClient();
    }
}
