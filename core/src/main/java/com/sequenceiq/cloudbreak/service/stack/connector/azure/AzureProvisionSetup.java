package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloud.azure.client.AzureClientUtil;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
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
    private static final int REQ_RETRY_COUNT = 5;

    @Inject
    private AzureStackUtil azureStackUtil;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;

    @Inject
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

    @Override
    public String preProvisionCheck(Stack stack) {
        Credential credential = stack.getCredential();
        azureStackUtil.migrateFilesIfNeeded((AzureCredential) credential);
        try {
            final AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) credential);
            getOsImages(azureClient);
        } catch (Exception ex) {
            if ("Forbidden".equals(ex.getMessage())) {
                return "Please upload your credential file to Azure portal.";
            } else {
                return ex.getMessage();
            }
        }
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private Map<Integer, String[]> createImages(Stack stack, AzureLocation azureLocation, AzureClient azureClient, String affinityGroupName) {
        Map<Integer, String[]> accountIndexKeys = new HashMap<>();
        int numStorageAccounts = azureStackUtil.getNumOfStorageAccounts(stack);
        if (numStorageAccounts == AzureStackUtil.GLOBAL_STORAGE) {
            accountIndexKeys = prepareStorageAccounts(stack, azureLocation, azureClient, affinityGroupName, numStorageAccounts);
        } else {
            for (int storageAccountIndex = 0; storageAccountIndex < numStorageAccounts; storageAccountIndex++) {
                accountIndexKeys.putAll(prepareStorageAccounts(stack, azureLocation, azureClient, affinityGroupName, storageAccountIndex));
            }
        }
        return accountIndexKeys;
    }

    private Map<Integer, String[]> prepareStorageAccounts(Stack stack, AzureLocation azureLocation,
            final AzureClient azureClient, String affinityGroupName, int storageAccountIndex) {
        Map<Integer, String[]> accountIndexKeys = new HashMap<>();
        final String osImageName = azureStackUtil.getOsImageName(stack, azureLocation, storageAccountIndex);
        LOGGER.info("Checking if image: {} exists in Azure image list.", osImageName);
        if (!isImageAvailable(azureClient, osImageName)) {
            String osStorageName = azureStackUtil.getOSStorageName(stack, azureLocation, storageAccountIndex);
            createOSStorage(stack, azureClient, osStorageName, affinityGroupName);
            String targetBlobContainerUri = "http://" + osStorageName + ".blob.core.windows.net/vm-images";
            String[] split = stack.getImage().split("/");
            String imageVHDName = split[split.length - 1];
            String targetImageUri = targetBlobContainerUri + '/' + imageVHDName;
            LOGGER.info("OS image destination will be: {}", targetImageUri);
            Map<String, String> params = new HashMap<>();
            params.put(AzureStackUtil.NAME, osStorageName);
            LOGGER.info("Starting to get a storage key on Azure.");
            String keyJson = getStorageAccountKeys(azureClient, params);
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
            LOGGER.info("Image: {} already exists, no need to copy it.", osImageName);
        }
        return accountIndexKeys;
    }

    private void createImageLinks(Stack stack, AzureLocation azureLocation, AzureClient azureClient, Map<Integer, String[]> accountIndexKeys) {
        for (int storageIndex : accountIndexKeys.keySet()) {
            String[] accountKeys = accountIndexKeys.get(storageIndex);
            String targetImageUri = accountKeys[1];
            checkCopyStatus(stack, targetImageUri, accountKeys[0]);
            createOsImageLink(stack, storageIndex, azureClient, targetImageUri, azureLocation);
        }
    }

    private void createOsImageLink(Stack stack, int storageIndex, AzureClient azureClient, String targetImageUri, AzureLocation location) {
        LOGGER.info("Starting to create a new Os image LINK on Azure for image: {}", targetImageUri);
        Map<String, String> params;
        params = new HashMap<>();
        params.put(AzureStackUtil.NAME, azureStackUtil.getOsImageName(stack, location, storageIndex));
        params.put(OS, "Linux");
        params.put(MEDIALINK, targetImageUri);
        addOsImage(azureClient, params, REQ_RETRY_COUNT);
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
            stackUpdater.updateStackStatusReason(stack.getId(), String.format("The copy status is: %s%%.", copyPercentage));
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

    private void createOSStorage(Stack stack, final AzureClient azureClient, final String storageName, String affinityGroupName) {
        try {
            LOGGER.info("Starting to create storage with {} name", storageName);
            getStorageAccount(azureClient, storageName);
            LOGGER.info("Storage already exist no need to create it with {} name", storageName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, storageName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(AFFINITYGROUP, affinityGroupName);
                HttpResponseDecorator response = createStorageAccount(azureClient, params);
                AzureResourcePollerObject azureResourcePollerObject =
                        new AzureResourcePollerObject(azureClient, ResourceType.AZURE_STORAGE, storageName, stack, response);
                azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
                LOGGER.info("Storage creation was successful with {} name", storageName);
            } else {
                LOGGER.error(String.format("Failure occurred during storage creation: {}", stack.getId()), ex);
                throw new AzureResourceException(ex);
            }
        }
    }

    private void createAffinityGroup(Stack stack, final AzureClient azureClient, final String affinityGroupName) {
        try {
            LOGGER.info("Starting to create affinity group with {} name", affinityGroupName);
            getAffinityGroup(azureClient, affinityGroupName);
            LOGGER.info("Affinity group already exists, no need to create it with {} name", affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(LOCATION, AzureLocation.valueOf(stack.getRegion()).region());
                createAffinityGroup(azureClient, params);
                LOGGER.info("Affinity group creation was success with {} name", affinityGroupName);
            } else {
                LOGGER.error("Error creating affinity group: {}, stack Id: {}", affinityGroupName, stack.getId(), ex);
                throw new AzureResourceException(ex);
            }
        }
    }

    private void getOsImages(final AzureClient azureClient) {
        execute(new Executor<Object>() {
            @Override
            public Object execute(Map<String, String> context, String name) {
                return azureClient.getOsImages();
            }

            @Override
            public String getRequestName() {
                return "getOsImages";
            }
        });
    }

    private boolean isImageAvailable(final AzureClient azureClient, String osImageName) {
        return execute(osImageName, new Executor<Boolean>() {
            @Override
            public Boolean execute(Map<String, String> context, String name) {
                return azureClient.isImageAvailable(name);
            }

            @Override
            public String getRequestName() {
                return "isImageAvailable";
            }
        });
    }

    private String getStorageAccountKeys(final AzureClient azureClient, Map<String, String> params) {
        return execute(params, new Executor<String>() {
            @Override
            public String execute(Map<String, String> context, String name) {
                return (String) azureClient.getStorageAccountKeys(context);
            }

            @Override
            public String getRequestName() {
                return "getStorageAccountKeys";
            }
        });
    }

    private void addOsImage(final AzureClient azureClient, final Map<String, String> params, int retryCount) {
        execute(params, new Executor<Object>() {
            @Override
            public Object execute(Map<String, String> context, String name) {
                return azureClient.addOsImage(context);
            }

            @Override
            public String getRequestName() {
                return "addOsImage";
            }
        });
    }

    private void getStorageAccount(final AzureClient azureClient, String storageName) {
        execute(storageName, new Executor<Object>() {
            @Override
            public Object execute(Map<String, String> context, String name) {
                return azureClient.getStorageAccount(name);
            }

            @Override
            public String getRequestName() {
                return "getStorageAccount";
            }
        });
    }

    private HttpResponseDecorator createStorageAccount(final AzureClient azureClient, Map<String, String> params) {
        return execute(params, new Executor<HttpResponseDecorator>() {
            @Override
            public HttpResponseDecorator execute(Map<String, String> context, String name) {
                return (HttpResponseDecorator) azureClient.createStorageAccount(context);
            }

            @Override
            public String getRequestName() {
                return "createStorageAccount";
            }
        });
    }

    private void getAffinityGroup(final AzureClient azureClient, String affinityGroupName) {
        execute(affinityGroupName, new Executor<Object>() {
            @Override
            public Object execute(Map<String, String> context, String name) {
                return azureClient.getAffinityGroup(name);
            }

            @Override
            public String getRequestName() {
                return "getAffinityGroup";
            }
        });
    }

    private void createAffinityGroup(final AzureClient azureClient, Map<String, String> params) {
        execute(params, new Executor<Object>() {
            @Override
            public Object execute(Map<String, String> context, String name) {
                return azureClient.createAffinityGroup(context);
            }

            @Override
            public String getRequestName() {
                return "createAffinityGroup";
            }
        });
    }

    private <T> T execute(Executor<T> executor) {
        return execute(Collections.<String, String>emptyMap(), executor);
    }

    private <T> T execute(Map<String, String> context, Executor<T> executor) {
        return execute(context, null, executor, REQ_RETRY_COUNT);
    }

    private <T> T execute(String name, Executor<T> executor) {
        return execute(null, name, executor, REQ_RETRY_COUNT);
    }

    private <T> T execute(Map<String, String> context, String name, Executor<T> executor, int retryCount) {
        try {
            return executor.execute(context, name);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to execute Azure request %s due to %s", executor.getRequestName(), e.getMessage());
            if (retryCount > 0 && e instanceof SocketTimeoutException) {
                LOGGER.warn(errorMessage);
                return execute(context, name, executor, --retryCount);
            }
            LOGGER.error(errorMessage, e);
            throw e;
        }
    }

    private interface Executor<T> {
        T execute(Map<String, String> context, String name);

        String getRequestName();
    }

}
