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
        LOGGER.info("Checking image exist in Azure image list.");
        if (!azureClient.isImageAvailable(azureStackUtil.getOsImageName(credential, azureLocation, stack.getImage()))) {
            String affinityGroupName = ((AzureCredential) credential).getCommonName(azureLocation);
            LOGGER.info("Starting create affinitygroup with {} name", affinityGroupName);
            createAffinityGroup(stack, azureClient, affinityGroupName);
            String storageName = String.format("%s%s", VM_COMMON_NAME, stack.getId());
            LOGGER.info("Starting create storage with {} name", storageName);
            createStorage(stack, azureClient, affinityGroupName);
            String targetBlobContainerUri = "http://" + affinityGroupName + ".blob.core.windows.net/vm-images";
            String targetImageUri = targetBlobContainerUri + '/' + storageName + ".vhd";
            LOGGER.info("Image destination will be: {}", targetImageUri);
            Map<String, String> params = new HashMap<>();
            params.put(AzureStackUtil.NAME, affinityGroupName);
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
            checkCopyStatus(stack, targetImageUri, storageAccountKey);
            LOGGER.info("Starting to create a new Os image LINK on Azure");
            createOsImageLink(credential, azureClient, targetImageUri, azureLocation, stack.getImage());
            LOGGER.info("Image creation was success on Azure");
        } else {
            LOGGER.info("Image already exist no need to copy it.");
        }
        return new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                .withSetupProperty(CREDENTIAL, stack.getCredential());
    }

    private void createOsImageLink(Credential credential, AzureClient azureClient, String targetImageUri, AzureLocation location, String imageUrl) {
        Map<String, String> params;
        params = new HashMap<>();
        params.put(AzureStackUtil.NAME, azureStackUtil.getOsImageName(credential, location, imageUrl));
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
                LOGGER.info("Interrupted exception occured during sleep.", e);
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
                    LOGGER.info("Error occured when created blob container.", ex);
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

    private void createStorage(Stack stack, AzureClient azureClient, String affinityGroupName) {
        try {
            azureClient.getStorageAccount(affinityGroupName);
            LOGGER.info("Storage already exist no need to create it with {} name", affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(AFFINITYGROUP, affinityGroupName);
                HttpResponseDecorator response = (HttpResponseDecorator) azureClient.createStorageAccount(params);
                AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(azureClient, stack, response);
                azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
                LOGGER.info("Storage storage was success with {} name", affinityGroupName);
            } else {
                LOGGER.error(String.format("Failure during storage creation: {}", stack.getId()), ex);
                throw new AzureResourceException(ex);
            }
        }
    }

    private void createAffinityGroup(Stack stack, AzureClient azureClient, String affinityGroupName) {
        try {
            azureClient.getAffinityGroup(affinityGroupName);
            LOGGER.info("Affinitygroup exist no need to create it with {} name", affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(LOCATION, AzureLocation.valueOf(stack.getRegion()).region());
                azureClient.createAffinityGroup(params);
                LOGGER.info("Affinitygroup creation was success with {} name", affinityGroupName);
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
