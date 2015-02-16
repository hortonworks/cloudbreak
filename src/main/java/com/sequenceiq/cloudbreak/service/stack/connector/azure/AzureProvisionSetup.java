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
import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloud.azure.client.AzureClientUtil;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
import groovyx.net.http.ResponseParseException;
import reactor.core.Reactor;
import reactor.event.Event;

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
    private Reactor reactor;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;

    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public void setupProvisioning(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        Credential credential = stack.getCredential();
        AzureLocation azureLocation = AzureLocation.valueOf(stack.getRegion());
        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) credential);
        if (!azureClient.isImageAvailable(azureStackUtil.getOsImageName(credential, azureLocation, stack.getImage()))) {
            String affinityGroupName = ((AzureCredential) credential).getCommonName(azureLocation);
            createAffinityGroup(stack, azureClient, affinityGroupName);
            String storageName = String.format("%s%s", VM_COMMON_NAME, stack.getId());
            createStorage(stack, azureClient, affinityGroupName);
            String targetBlobContainerUri = "http://" + affinityGroupName + ".blob.core.windows.net/vm-images";
            String targetImageUri = targetBlobContainerUri + '/' + storageName + ".vhd";
            Map<String, String> params = new HashMap<>();
            params.put(AzureStackUtil.NAME, affinityGroupName);
            String keyJson = (String) azureClient.getStorageAccountKeys(params);
            JsonNode actualObj = null;
            try {
                actualObj = MAPPER.readValue(keyJson, JsonNode.class);
            } catch (IOException e) {
                LOGGER.info("Can not read Json node: ", e);
                throw new InternalServerException("Can not read Json node: ", e);
            }
            String storageAccountKey = actualObj.get("StorageService").get("StorageServiceKeys").get("Primary").asText();

            createBlobContainer(targetBlobContainerUri, storageAccountKey);
            AzureClientUtil.copyOsImage(storageAccountKey, stack.getImage(), targetImageUri);

            checkCopyStatus(stack, targetImageUri, storageAccountKey);
            createOsImageLink(credential, azureClient, targetImageUri, azureLocation, stack.getImage());
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT,
                Event.wrap(new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                                .withSetupProperty(CREDENTIAL, stack.getCredential())
                )
        );
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
            LOGGER.info(String.format("copy progress=%s / %s percentage: %s%%.",
                    copyStatusFromServer.get("copiedBytes"),
                    copyStatusFromServer.get("totalBytes"),
                    copyPercentage));

            retryingStackUpdater.updateStackStatusReason(stack.getId(), String.format("The copy status is: %s%%.", copyPercentage));
            try {
                Thread.sleep(MILLIS);
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception occured during sleep.", e);
                Thread.currentThread().interrupt();
            }
        }
        if (!SUCCESS.equals(copyStatus)) {
            throw new InternalServerException("Copy OS image failed with status: " + copyStatus);
        }
    }

    private void createBlobContainer(String targetBlobContainerUri, String storageAccountKey) {
        try {
            AzureClientUtil.createBlobContainer(storageAccountKey, targetBlobContainerUri);
        } catch (Exception ex) {
            if (ex instanceof ResponseParseException) {
                if (((ResponseParseException) ex).getStatusCode() != CONTAINER_EXISTS) {
                    throw ex;
                }
            } else {
                throw ex;
            }
        }
    }

    @Override
    public Optional<String> preProvisionCheck(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        Credential credential = stack.getCredential();
        azureStackUtil.migrateFilesIfNeeded((AzureCredential) credential);
        try {
            AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) credential);
            Object osImages = azureClient.getOsImages();
        } catch (Exception ex) {
            if ("Forbidden".equals(ex.getMessage())) {
                return Optional.of("Please upload your credential file to Azure portal.");
            } else {
                return Optional.of(ex.getMessage());
            }
        }
        return Optional.absent();
    }

    private void createStorage(Stack stack, AzureClient azureClient, String affinityGroupName) {
        MDCBuilder.buildMdcContext(stack);
        try {
            azureClient.getStorageAccount(affinityGroupName);
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
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", stack.getId()), ex);
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    private void createAffinityGroup(Stack stack, AzureClient azureClient, String affinityGroupName) {
        MDCBuilder.buildMdcContext(stack);
        try {
            azureClient.getAffinityGroup(affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(LOCATION, AzureLocation.valueOf(stack.getRegion()).location());
                azureClient.createAffinityGroup(params);
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the affinity group creation", stack.getId()), ex);
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private Map<String, Object> getSetupProperties(Stack stack) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CREDENTIAL, stack.getCredential());
        return properties;
    }

}
