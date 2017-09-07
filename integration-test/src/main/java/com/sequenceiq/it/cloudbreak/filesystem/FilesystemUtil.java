package com.sequenceiq.it.cloudbreak.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.Storage.Builder;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.oauth2.AccessTokenProvider;
import com.microsoft.azure.datalake.store.oauth2.ClientCredsTokenProvider;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.sequenceiq.it.util.ResourceUtil;



public class FilesystemUtil {

    private static final String AZURE_TOKEN_URL = "https://login.microsoftonline.com/";

    private static final String STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_write";

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemUtil.class);

    private FilesystemUtil() {
    }

    static void cleanUpFiles(ApplicationContext applicationContext, Map<String, String> cloudProviderParams,
            String fsType, String fsName, String folderPrefix, String containerName) throws IOException, GeneralSecurityException {
        switch (fsType) {
            case "ADLS":
                deleteAdlsObjects(cloudProviderParams, fsName, folderPrefix);
                break;
            case "GCS":
                deleteGcsObjects(applicationContext, cloudProviderParams, fsName, folderPrefix);
                break;
            case "WASB":
                deleteWasbContainer(cloudProviderParams, fsName, containerName);
                break;
            default:
                LOGGER.info("Filesystem type {} is not supported!", fsType);
                break;
        }
    }

    static void createWasbContainer(Map<String, String> cloudProviderParams, String accountName, String containerName) {
        try {
            String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=" + accountName + ";AccountKey="
                    + cloudProviderParams.get("accountKeyWasb");
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            container.createIfNotExists();
        } catch (URISyntaxException e) {
            LOGGER.error("Error during creating the Wasb container, wrong URI syntax ", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Error during creating the Wasb container, invalid key ", e);
        } catch (StorageException e) {
            LOGGER.error("Error during creating the Wasb container, problem with storage ", e);
        }
    }

    private static void deleteWasbContainer(Map<String, String> cloudProviderParams, String accountName, String containerName) {
        try {
            String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=" + accountName + ";AccountKey="
                    + cloudProviderParams.get("accountKeyWasb");
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            container.deleteIfExists();
        } catch (URISyntaxException e) {
            LOGGER.error("Error during deleting the Wasb container, wrong URI syntax ", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Error during deleting the Wasb container, invalid key ", e);
        } catch (StorageException e) {
            LOGGER.error("Error during deleting the Wasb container, problem with storage ", e);
        }
    }

    private static void deleteGcsObjects(ApplicationContext applicationContext, Map<String, String> cloudProviderParams, String bucketName,
            String folderPrefix) throws IOException, GeneralSecurityException {
        String serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, cloudProviderParams.get("p12File"));
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                new ByteArrayInputStream(Base64.decodeBase64(serviceAccountPrivateKey)), "notasecret", "privatekey", "notasecret");
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential googleCredential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(cloudProviderParams.get("serviceAccountId"))
                .setServiceAccountScopes(Collections.singletonList(STORAGE_SCOPE))
                .setServiceAccountPrivateKey(privateKey)
                .build();

        Storage storage = new Builder(httpTransport, jsonFactory, googleCredential)
                .setApplicationName("Google-BucketsInsertExample/1.0").build();

        List<StorageObject> storageObjects = new ArrayList<>();
        Storage.Objects.List listObjects = storage.objects().list(bucketName).setPrefix(folderPrefix);
        Objects objects = listObjects.execute();

        Assert.assertNotNull(objects.getItems(), "Not found any objects with " + folderPrefix + " prefix.");

        storageObjects.addAll(objects.getItems());
        for (StorageObject storageObject : storageObjects) {
            storage.objects().delete("hm-bucket", storageObject.getName()).execute();
        }
    }

    private static void deleteAdlsObjects(Map<String, String> cloudProviderParams, String filesystemName, String folderPrefix) throws IOException {
        String authTokenEndpoint = AZURE_TOKEN_URL + cloudProviderParams.get("tenantId") + "/oauth2/token";
        AccessTokenProvider provider = new ClientCredsTokenProvider(authTokenEndpoint, cloudProviderParams.get("accesKey"),
                cloudProviderParams.get("secretKey"));
        ADLStoreClient client = ADLStoreClient.createClient(filesystemName + ".azuredatalakestore.net", provider);
        if (client.checkExists(folderPrefix)) {
            client.deleteRecursive(folderPrefix);
        }
    }
}