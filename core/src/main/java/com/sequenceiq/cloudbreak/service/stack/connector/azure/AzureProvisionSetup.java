package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloud.azure.client.AzureClientUtil;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
import groovyx.net.http.ResponseParseException;

@Component
public class AzureProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureProvisionSetup.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DESCRIPTION = "description";
    private static final String AFFINITYGROUP = "affinityGroup";
    private static final String LOCATION = "location";
    private static final String VM_COMMON_NAME = "cloudbreak";
    private static final String OS = "os";
    private static final String MEDIALINK = "mediaLink";
    private static final int MILLIS = 5000;
    private static final String PENDING = "pending";
    private static final String SUCCESS = "success";
    private static final int ONE_HUNDRED = 100;
    private static final int CONTAINER_EXISTS = 409;
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 60;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;

    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        Credential credential = stack.getCredential();
        AzureLocation azureLocation = AzureLocation.valueOf(stack.getRegion());
        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) credential);

        String affinityGroupName = ((AzureCredential) credential).getAffinityGroupName(azureLocation);
        createAffinityGroup(stack, azureClient, affinityGroupName);

        Map<Integer, String[]> accountIndexKeys = createImages(stack, azureLocation, azureClient, affinityGroupName);
        createImageLinks(stack, azureLocation, azureClient, accountIndexKeys);
        return new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                .withSetupProperty(CREDENTIAL, stack.getCredential());
    }

    private Map<Integer, String[]> createImages(Stack stack, AzureLocation azureLocation, AzureClient azureClient, String affinityGroupName) {
        Map<Integer, String[]> accountIndexKeys = new HashMap<>();
        int numStorageAccounts = azureStackUtil.getNumOfStorageAccount(stack);
        for (int storageAccountIndex = 0; storageAccountIndex < numStorageAccounts; storageAccountIndex++) {
            LOGGER.info("Checking image exists in Azure image list.");
            String osImageName = azureStackUtil.getOsImageName(storageAccountIndex, azureLocation, stack.getImage());
            if (!azureClient.isImageAvailable(osImageName)) {
                String osStorageName = azureStackUtil.getOSStorageName(azureLocation, storageAccountIndex);
                createOSStorage(stack, azureClient, osStorageName, affinityGroupName);
                String targetBlobContainerUri = "http://" + osStorageName + ".blob.core.windows.net/vm-images";
                String[] split = stack.getImage().split("/");
                String imageVHDName = split[split.length - 1];
                String targetImageUri = targetBlobContainerUri + '/' + imageVHDName;
                LOGGER.info("OS image destination will be: {}", targetImageUri);
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, osStorageName);
                LOGGER.info("Starting to get a storage key on Azure.");
                String keyJson = (String) azureClient.getStorageAccountKeys(params);
                JsonNode actualObj = null;
                try {
                    actualObj = MAPPER.readValue(keyJson, JsonNode.class);
                } catch (IOException e) {
                    LOGGER.info("Can not read Json node which was returned from Azure: ", e);
                    throw new AzureResourceException(e);
                }
                String storageAccountKey = actualObj.get("StorageService").get("StorageServiceKeys").get("Primary").asText();
                LOGGER.info(String.format("Storage key will be: %s", storageAccountKey));
                LOGGER.info("Starting to create a new blob-container on Azure");
                createBlobContainer(targetBlobContainerUri, storageAccountKey);
                LOGGER.info("Starting to copy a new Os image on Azure");
                AzureClientUtil.copyOsImage(storageAccountKey, stack.getImage(), targetImageUri);
                accountIndexKeys.put(storageAccountIndex, new String[]{storageAccountKey, targetImageUri});
            } else {
                LOGGER.info("Image: {} already exist no need to copy it.", osImageName);
            }
        }
        return accountIndexKeys;
    }

    private void createImageLinks(Stack stack, AzureLocation azureLocation, AzureClient azureClient, Map<Integer, String[]> accountIndexKeys) {
        for (int storageIndex : accountIndexKeys.keySet()) {
            String[] accountKeys = accountIndexKeys.get(storageIndex);
            String targetImageUri = accountKeys[1];
            checkCopyStatus(stack, targetImageUri, accountKeys[0]);
            createOsImageLink(storageIndex, azureClient, targetImageUri, azureLocation, stack.getImage());
        }
    }

    private void createOsImageLink(int storageIndex, AzureClient azureClient, String targetImageUri, AzureLocation location, String imageUrl) {
        LOGGER.info("Starting to create a new Os image LINK on Azure for image: {}", targetImageUri);
        Map<String, String> params;
        params = new HashMap<>();
        params.put(AzureStackUtil.NAME, azureStackUtil.getOsImageName(storageIndex, location, imageUrl));
        params.put(OS, "Linux");
        params.put(MEDIALINK, targetImageUri);
        azureClient.addOsImage(params);
    }

    private void checkCopyStatus(Stack stack, String targetImageUri, String storageAccountKey) {
        String copyStatus = PENDING;
        while (PENDING.equals(copyStatus)) {
            Map<String, String> copyStatusFromServer = (Map<String, String>) AzureClientUtil.getCopyOsImageProgress(storageAccountKey, targetImageUri);
            copyStatus = copyStatusFromServer.get("status");
            Long copied = Long.valueOf(copyStatusFromServer.get("copiedBytes"));
            Long total = Long.valueOf(copyStatusFromServer.get("totalBytes"));
            double copyPercentage = (long) ((float) copied / total * ONE_HUNDRED);
            LOGGER.info(String.format("copy progress=%s / %s percentage: %s%%.", copied, total, copyPercentage));
            retryingStackUpdater.updateStackStatusReason(stack.getId(), String.format("The copy status is: %s%%.", copyPercentage));
            try {
                Thread.sleep(MILLIS);
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception occurred during sleep.", e);
                Thread.currentThread().interrupt();
            }
        }
        if (!SUCCESS.equals(copyStatus)) {
            throw new AzureResourceException("Copy OS image failed with status: " + copyStatus);
        }
    }

    private void createBlobContainer(String targetBlobContainerUri, String storageAccountKey) {
        try {
            AzureClientUtil.createBlobContainer(storageAccountKey, targetBlobContainerUri);
            LOGGER.info("Blob-container creation was success.");
        } catch (Exception ex) {
            if (ex instanceof ResponseParseException) {
                if (((ResponseParseException) ex).getStatusCode() != CONTAINER_EXISTS) {
                    LOGGER.info("Error occurred when created blob container.", ex);
                    throw ex;
                } else {
                    LOGGER.info("Blob container already exist no need to create it.");
                }
            } else {
                throw ex;
            }
        }
    }

    @Override
    public String preProvisionCheck(Stack stack) {
        Credential credential = stack.getCredential();
        azureStackUtil.migrateFilesIfNeeded((AzureCredential) credential);
        try {
            AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) credential);
            Object osImages = azureClient.getOsImages();
        } catch (Exception ex) {
            if ("Forbidden".equals(ex.getMessage())) {
                return "Please upload your credential file to Azure portal.";
            } else {
                return ex.getMessage();
            }
        }
        return null;
    }

    private void createOSStorage(Stack stack, AzureClient azureClient, String storageName, String affinityGroupName) {
        try {
            LOGGER.info("Starting to create storage with {} name", storageName);
            azureClient.getStorageAccount(storageName);
            LOGGER.info("Storage already exist no need to create it with {} name", storageName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, storageName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(AFFINITYGROUP, affinityGroupName);
                HttpResponseDecorator response = (HttpResponseDecorator) azureClient.createStorageAccount(params);
                AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(azureClient, stack, response);
                azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
                LOGGER.info("Storage creation was successful with {} name", storageName);
            } else {
                LOGGER.error(String.format("Failure occurred during storage creation: {}", stack.getId()), ex);
                throw new AzureResourceException(ex);
            }
        }
    }

    private void createAffinityGroup(Stack stack, AzureClient azureClient, String affinityGroupName) {
        try {
            LOGGER.info("Starting to create affinity group with {} name", affinityGroupName);
            azureClient.getAffinityGroup(affinityGroupName);
            LOGGER.info("Affinity group already exists, no need to create it with {} name", affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(LOCATION, AzureLocation.valueOf(stack.getRegion()).region());
                azureClient.createAffinityGroup(params);
                LOGGER.info("Affinity group creation was success with {} name", affinityGroupName);
            } else {
                LOGGER.error("Error creating affinity group: {}, stack Id: {}", affinityGroupName, stack.getId(), ex);
                throw new AzureResourceException(ex);
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

}
