package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.EMAILASFOLDER;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloud.azure.client.AzureClientUtil;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
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

    @Value("${cb.azure.image.uri}")
    private String baseImageUri;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Autowired
    private WebsocketService websocketService;

    @Override
    public void setupProvisioning(Stack stack) {
        Credential credential = stack.getCredential();
        String emailAsFolder = azureStackUtil.emailAsFolder(stack.getOwner());

        String filePath = AzureCertificateService.getUserJksFileName(credential, emailAsFolder);
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        if (!azureClient.isImageAvailable(azureStackUtil.getOsImageName(credential))) {
            String affinityGroupName = ((AzureCredential) credential).getCommonName();
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

            AzureClientUtil.createBlobContainer(storageAccountKey, targetBlobContainerUri);
            AzureClientUtil.copyOsImage(storageAccountKey, baseImageUri, targetImageUri);

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

                websocketService.sendToTopicUser(stack.getOwner(), WebsocketEndPoint.COPY_IMAGE,
                        new StatusMessage(stack.getId(), stack.getName(), PENDING, String.format("The copy status is: %s%%.", copyPercentage)));
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
            params = new HashMap<>();
            params.put(AzureStackUtil.NAME, azureStackUtil.getOsImageName(credential));
            params.put(OS, "Linux");
            params.put(MEDIALINK, targetImageUri);
            azureClient.addOsImage(params);
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT,
                Event.wrap(new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                                .withSetupProperty(CREDENTIAL, stack.getCredential())
                                .withSetupProperty(EMAILASFOLDER, emailAsFolder)
                )
        );
    }

    private void createStorage(Stack stack, AzureClient azureClient, String affinityGroupName) {
        try {
            azureClient.getStorageAccount(affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(AFFINITYGROUP, affinityGroupName);
                HttpResponseDecorator response = (HttpResponseDecorator) azureClient.createStorageAccount(params);
                azureClient.waitUntilComplete((String) azureClient.getRequestId(response));
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", stack.getId()), ex);
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    private void createAffinityGroup(Stack stack, AzureClient azureClient, String affinityGroupName) {
        try {
            azureClient.getAffinityGroup(affinityGroupName);
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                params.put(DESCRIPTION, VM_COMMON_NAME);
                params.put(LOCATION, ((AzureTemplate) stack.getTemplate()).getLocation().location());
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

    @Override
    public Map<String, Object> getSetupProperties(Stack stack) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CREDENTIAL, stack.getCredential());
        properties.put(EMAILASFOLDER, azureStackUtil.emailAsFolder(stack.getOwner()));
        return properties;
    }

    @Override
    public Map<String, String> getUserDataProperties(Stack stack) {
        return new HashMap<>();
    }
}
