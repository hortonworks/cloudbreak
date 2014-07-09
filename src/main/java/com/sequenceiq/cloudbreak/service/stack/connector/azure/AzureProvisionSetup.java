package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.EMAILASFOLDER;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;

import java.io.File;
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
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterCreationFailure;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

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

    @Value("${azure.image.v1}")
    private String baseImageUri;

    @Autowired
    private Reactor reactor;

    @Override
    public void setupProvisioning(Stack stack) {
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
        Credential credential = stack.getCredential();
        String emailAsFolder = stack.getUser().emailAsFolder();

        String filePath = AzureCertificateService.getUserJksFileName(credential, emailAsFolder);
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(), file.getAbsolutePath(), ((AzureCredential) credential).getJks()
        );
        if (!azureClient.isImageAvailable(AzureStackUtil.getOsImageName(credential))) {
            String affinityGroupName = ((AzureCredential) credential).getName().replaceAll("\\s+", "");
            try {
                azureClient.getAffinityGroup(affinityGroupName);
            } catch (Exception ex) {
                if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                    Map<String, String> params = new HashMap<>();
                    params.put(AzureStackUtil.NAME, affinityGroupName);
                    params.put(DESCRIPTION, VM_COMMON_NAME);
                    params.put(LOCATION, "East US");
                    azureClient.createAffinityGroup(params);
                } else {
                    LOGGER.info("There was a problem with the creation of the affinity group.");
                    reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new ClusterCreationFailure(stack.getId(),
                            "The copy of the os image was not success")));
                }
            }
            String storageName = String.format("%s%s", VM_COMMON_NAME, stack.getId());
            try {
                azureClient.getStorageAccount(affinityGroupName);
            } catch (Exception ex) {
                if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                    Map<String, String> params = new HashMap<>();
                    params.put(AzureStackUtil.NAME, affinityGroupName);
                    params.put(DESCRIPTION, VM_COMMON_NAME);
                    params.put(AFFINITYGROUP, affinityGroupName);
                    HttpResponseDecorator response = (HttpResponseDecorator) azureClient.createStorageAccount(params);
                    azureClient.waitUntilComplete((String) azureClient.getRequestId(response));
                } else {
                    LOGGER.info("There was a problem with the creation of the storage.");
                    reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new ClusterCreationFailure(stack.getId(),
                            "The copy of the os image was not success")));
                }

            }
            try {
                String targetBlobContainerUri = "http://" + affinityGroupName + ".blob.core.windows.net/vm-images";
                String targetImageUri = targetBlobContainerUri + '/' + storageName + ".vhd";
                Map<String, String> params = new HashMap<>();
                params.put(AzureStackUtil.NAME, affinityGroupName);
                String keyJson = (String) azureClient.getStorageAccountKeys(params);

                JsonNode actualObj = MAPPER.readValue(keyJson, JsonNode.class);
                String storageAccountKey = actualObj.get("StorageService").get("StorageServiceKeys").get("Primary").asText();

                AzureClientUtil.createBlobContainer(storageAccountKey, targetBlobContainerUri);
                AzureClientUtil.copyOsImage(storageAccountKey, baseImageUri, targetImageUri);
                AzureClientUtil.imageCopyProgress(storageAccountKey, targetImageUri);
                params = new HashMap<>();
                params.put(AzureStackUtil.NAME, AzureStackUtil.getOsImageName(credential));
                params.put(OS, "Linux");
                params.put(MEDIALINK, targetImageUri);
                azureClient.addOsImage(params);
            } catch (IOException e) {
                reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new ClusterCreationFailure(stack.getId(),
                        "There was a problem with the Json node parsing when tried to create the specific image.")));
            }
        }

        reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT,
                Event.wrap(new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                                .withSetupProperty(CREDENTIAL, stack.getCredential())
                                .withSetupProperty(EMAILASFOLDER, stack.getUser().emailAsFolder())
                )
        );
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
