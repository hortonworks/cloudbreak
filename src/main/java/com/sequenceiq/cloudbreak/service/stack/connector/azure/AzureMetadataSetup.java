package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataUpdateComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AzureMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMetadataSetup.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Autowired
    private AzureMetadataSetupCheckerTask azureMetadataSetupCheckerTask;
    @Autowired
    private PollingService<AzureMetadataSetupCheckerTaskContext> azureMetadataSetupCheckerTaskPollingService;
    @Autowired
    private Reactor reactor;
    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public void setupMetadata(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = azureStackUtil.createAzureClient(azureCredential);
        Set<CoreInstanceMetaData> instanceMetaDatas = collectMetaData(stack, azureClient);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.AZURE, stack.getId(), instanceMetaDatas)));
    }

    @Override
    public void addNewNodesToMetadata(Stack stack, Set<Resource> resourceList, String hostGroup) {
        MDCBuilder.buildMdcContext(stack);
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = azureStackUtil.createAzureClient(azureCredential);
        List<Resource> resources = new ArrayList<>();
        for (Resource resource : resourceList) {
            if (ResourceType.AZURE_VIRTUAL_MACHINE.equals(resource.getResourceType())) {
                resources.add(resource);
            }
        }
        Set<CoreInstanceMetaData> instanceMetaDatas = collectMetaData(stack, azureClient, resources);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT,
                Event.wrap(new MetadataUpdateComplete(CloudPlatform.AZURE, stack.getId(), instanceMetaDatas, hostGroup)));
    }

    private Set<CoreInstanceMetaData> collectMetaData(Stack stack, AzureClient azureClient, List<Resource> resources) {
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        for (Resource resource : resources) {
            instanceMetaDatas.add(getMetadata(stack, azureClient, resource));
        }
        return instanceMetaDatas;
    }

    private Set<CoreInstanceMetaData> collectMetaData(Stack stack, AzureClient azureClient) {
        return collectMetaData(stack, azureClient, stack.getResourcesByType(ResourceType.AZURE_VIRTUAL_MACHINE));
    }

    private CoreInstanceMetaData getMetadata(Stack stack, AzureClient azureClient, Resource resource) {
        MDCBuilder.buildMdcContext(stack);
        Map<String, Object> props = new HashMap<>();
        props.put(NAME, resource.getResourceName());
        props.put(SERVICENAME, resource.getResourceName());
        AzureMetadataSetupCheckerTaskContext azureMetadataSetupCheckerTaskContext = new AzureMetadataSetupCheckerTaskContext(azureClient, stack, props);
        azureMetadataSetupCheckerTaskPollingService.pollWithTimeout(azureMetadataSetupCheckerTask, azureMetadataSetupCheckerTaskContext,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        Object virtualMachine = azureClient.getVirtualMachine(props);
        try {
            CoreInstanceMetaData instanceMetaData = new CoreInstanceMetaData(
                    resource.getResourceName(),
                    getPrivateIP((String) virtualMachine),
                    resource.getResourceName() + ".cloudapp.net",
                    stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()).getTemplate().getVolumeCount(),
                    getLongName((String) virtualMachine),
                    resource.getInstanceGroup()
                    );
            return instanceMetaData;
        } catch (IOException e) {
            LOGGER.error(String.format("Instance %s is not reachable: %s", resource.getResourceName(), e.getMessage()), e);
        }
        return null;
    }

    @VisibleForTesting
    protected String getVirtualIP(String response) throws IOException {
        JsonNode actualObj = MAPPER.readValue(response, JsonNode.class);
        return actualObj.get("Deployment").get("VirtualIPs").get("VirtualIP").get("Address").asText();
    }

    @VisibleForTesting
    protected String getLongName(String response) throws IOException {
        JsonNode actualObj = MAPPER.readValue(response, JsonNode.class);
        String dns = actualObj.get("Deployment").get("InternalDnsSuffix").asText();
        String deploymentName = actualObj.get("Deployment").get("Name").asText();
        return String.format("%s.%s", deploymentName, dns);
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
