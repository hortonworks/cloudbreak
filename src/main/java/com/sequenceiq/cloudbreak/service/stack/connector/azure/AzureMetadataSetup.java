package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

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
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
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

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public void setupMetadata(Stack stack) {
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();

        String filePath = AzureCertificateService.getUserJksFileName(azureCredential,  stack.getUser().emailAsFolder());
        AzureClient azureClient = azureStackUtil.createAzureClient(azureCredential, filePath);
        String name = stack.getName().replaceAll("\\s+", "");
        Set<CoreInstanceMetaData> instanceMetaDatas = collectMetaData(stack, azureClient, name);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.AZURE, stack.getId(), instanceMetaDatas)));
    }

    private Set<CoreInstanceMetaData> collectMetaData(Stack stack, AzureClient azureClient, String name) {
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            Map<String, Object> props = new HashMap<>();
            props.put(NAME, resource.getResourceName());
            props.put(SERVICENAME, resource.getResourceName());
            Object virtualMachine = azureClient.getVirtualMachine(props);
            try {
                CoreInstanceMetaData instanceMetaData = new CoreInstanceMetaData(resource.getResourceName(),
                        getPrivateIP((String) virtualMachine),
                        getVirtualIP((String) virtualMachine));
                instanceMetaDatas.add(instanceMetaData);
            } catch (IOException e) {
                LOGGER.error(String.format("The instance %s was not reacheable: %s", resource.getResourceName(), e.getMessage()), e);
            }
        }
        return instanceMetaDatas;
    }

    @VisibleForTesting
    protected String getVirtualIP(String response) throws IOException {
        JsonNode actualObj = MAPPER.readValue(response, JsonNode.class);
        return actualObj.get("Deployment").get("VirtualIPs").get("VirtualIP").get("Address").asText();
    }

    @VisibleForTesting
    protected String getPrivateIP(String response) throws IOException {
        JsonNode actualObj = MAPPER.readValue(response, JsonNode.class);
        return actualObj.get("Deployment").get("RoleInstanceList").get("RoleInstance").get("IpAddress").asText();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
