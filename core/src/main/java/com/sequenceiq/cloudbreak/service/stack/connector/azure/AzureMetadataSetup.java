package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.DELETED;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.RUNNING;
import static com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState.STOPPED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

@Component
public class AzureMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMetadataSetup.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;
    private static final int MAX_FAILURE_COUNT = 3;

    @Inject
    private AzureMetadataSetupCheckerTask azureMetadataSetupCheckerTask;
    @Inject
    private PollingService<AzureMetadataSetupCheckerTaskContext> azureMetadataSetupCheckerTaskPollingService;
    @Inject
    private AzureStackUtil azureStackUtil;

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = azureStackUtil.createAzureClient(azureCredential);
        return collectMetaData(stack, azureClient);
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroup) {
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = azureStackUtil.createAzureClient(azureCredential);
        List<Resource> resources = new ArrayList<>();
        for (Resource resource : resourceList) {
            if (ResourceType.AZURE_VIRTUAL_MACHINE.equals(resource.getResourceType())) {
                resources.add(resource);
            }
        }
        return collectMetaData(stack, azureClient, resources);
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
        Map<String, Object> props = new HashMap<>();
        props.put(NAME, resource.getResourceName());
        props.put(SERVICENAME, resource.getResourceName());
        AzureMetadataSetupCheckerTaskContext azureMetadataSetupCheckerTaskContext = new AzureMetadataSetupCheckerTaskContext(azureClient, stack, props);
        PollingResult pollingResult = azureMetadataSetupCheckerTaskPollingService
                .pollWithTimeout(azureMetadataSetupCheckerTask,
                        azureMetadataSetupCheckerTaskContext,
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS,
                        MAX_FAILURE_COUNT);
        if (isSuccess(pollingResult)) {
            Object virtualMachine = azureClient.getVirtualMachine(props);
            try {
                CoreInstanceMetaData instanceMetaData = new CoreInstanceMetaData(
                        resource.getResourceName(),
                        getPrivateIP((String) virtualMachine),
                        getVirtualIP((String) virtualMachine),
                        stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()).getTemplate().getVolumeCount(),
                        stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup())
                );
                return instanceMetaData;
            } catch (IOException e) {
                LOGGER.error(String.format("Instance %s is not reachable: %s", resource.getResourceName(), e.getMessage()), e);
            }
        }
        return null;
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
    public InstanceSyncState getState(Stack stack, String instanceId) {
        Map<String, String> vmContext = createVMContext(instanceId);
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = azureStackUtil.createAzureClient(credential);
        InstanceSyncState instanceSyncState = IN_PROGRESS;
        try {
            if ("Running".equals(azureClient.getVirtualMachineState(vmContext))) {
                instanceSyncState = RUNNING;
            } else if ("Suspended".equals(azureClient.getVirtualMachineState(vmContext))) {
                instanceSyncState = STOPPED;
            }
        } catch (Exception ex) {
            instanceSyncState = DELETED;
        }
        return instanceSyncState;
    }

    private Map<String, String> createVMContext(String vmName) {
        Map<String, String> context = new HashMap<>();
        context.put(SERVICENAME, vmName);
        context.put(NAME, vmName);
        return context;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
