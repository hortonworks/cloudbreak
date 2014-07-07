package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.domain.CoreInstanceMetaData;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AzureMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMetadataSetup.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private Reactor reactor;

    @Override
    public void setupMetadata(Stack stack) {
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();

        String filePath = AzureCertificateService.getUserJksFileName(azureCredential,  stack.getUser().emailAsFolder());
        File file = new File(filePath);

        AzureClient azureClient = new AzureClient(
                azureCredential.getSubscriptionId(),
                file.getAbsolutePath(),
                azureCredential.getJks()
        );
        String name = stack.getName().replaceAll("\\s+", "");
        Set<CoreInstanceMetaData> instanceMetaDatas = collectMetaData(stack, azureClient, name);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.AZURE, stack.getId(), instanceMetaDatas)));
    }

    private Set<CoreInstanceMetaData> collectMetaData(Stack stack, AzureClient azureClient, String name) {
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = AzureStackUtil.getVmName(name, i);
            Map<String, Object> props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(SERVICENAME, vmName);
            Object virtualMachine = azureClient.getVirtualMachine(props);
            try {
                CoreInstanceMetaData instanceMetaData = new CoreInstanceMetaData(vmName,
                        getPrivateIP((String) virtualMachine),
                        getVirtualIP((String) virtualMachine));
                instanceMetaDatas.add(instanceMetaData);
            } catch (IOException e) {
                LOGGER.info("The instance {} was not reacheable: ", vmName, e.getMessage());
            }
        }
        return instanceMetaDatas;
    }


    private String getVirtualIP(String response) throws IOException {
        JsonNode actualObj = MAPPER.readValue(response, JsonNode.class);
        return actualObj.get("Deployment").get("VirtualIPs").get("VirtualIP").get("Address").asText();
    }

    private String getPrivateIP(String response) throws IOException {
        JsonNode actualObj = MAPPER.readValue(response, JsonNode.class);
        return actualObj.get("Deployment").get("RoleInstanceList").get("RoleInstance").get("IpAddress").asText();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
