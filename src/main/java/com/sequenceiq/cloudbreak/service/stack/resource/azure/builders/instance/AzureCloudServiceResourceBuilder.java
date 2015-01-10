package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureCloudServiceDeleteTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureCloudServiceDeleteTaskContext;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureDiskDeleteTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureDiskRemoveDeleteTaskContext;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;

@Component
@Order(1)
public class AzureCloudServiceResourceBuilder extends AzureSimpleInstanceResourceBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private AzureDiskDeleteTask azureDiskDeleteTask;
    @Autowired
    private PollingService<AzureDiskRemoveDeleteTaskContext> azureDiskRemoveReadyPollerObjectPollingService;
    @Autowired
    private AzureCloudServiceDeleteTask azureCloudServiceDeleteTask;
    @Autowired
    private PollingService<AzureCloudServiceDeleteTaskContext> azureCloudServiceRemoveReadyPollerObjectPollingService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest) throws Exception {
        AzureCloudServiceCreateRequest aCSCR = (AzureCloudServiceCreateRequest) createResourceRequest;
        HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createCloudService(aCSCR.getProps());
        String requestId = (String) aCSCR.getAzureClient().getRequestId(cloudServiceResponse);
        waitUntilComplete(aCSCR.getAzureClient(), requestId);
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject deleteContextObject) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureCloudServiceDeleteTaskContext azureCloudServiceDeleteTaskContext =
                new AzureCloudServiceDeleteTaskContext(deleteContextObject.getCommonName(), resource.getResourceName(),
                        stack, deleteContextObject.getNewAzureClient(credential));
        azureCloudServiceRemoveReadyPollerObjectPollingService
                .pollWithTimeout(azureCloudServiceDeleteTask, azureCloudServiceDeleteTaskContext, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);

        AzureClient azureClient = deleteContextObject.getNewAzureClient(credential);
        JsonNode actualObj = MAPPER.readValue((String) azureClient.getDisks(), JsonNode.class);
        List<String> disks = (List<String>) actualObj.get("Disks").findValues("Disk").get(0).findValuesAsText("Name");
        for (String jsonNode : disks) {
            if (jsonNode.startsWith(String.format("%s-%s-0", resource.getResourceName(), resource.getResourceName()))) {
                AzureDiskRemoveDeleteTaskContext azureDiskRemoveReadyPollerObject = new AzureDiskRemoveDeleteTaskContext(deleteContextObject.getCommonName(),
                        jsonNode,
                        stack, deleteContextObject.getNewAzureClient(credential));
                azureDiskRemoveReadyPollerObjectPollingService
                        .pollWithTimeout(azureDiskDeleteTask, azureDiskRemoveReadyPollerObject, POLLING_INTERVAL, 100);
            }
        }
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, AzureDescribeContextObject describeContextObject) throws Exception {
        Stack stack = stackRepository.findById(describeContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(SERVICENAME, resource.getResourceName());
        props.put(NAME, resource.getResourceName());
        try {
            AzureClient azureClient = describeContextObject.getNewAzureClient(credential);
            Object virtualMachine = azureClient.getVirtualMachine(props);
            return Optional.fromNullable(virtualMachine.toString());
        } catch (Exception ex) {
            return Optional.fromNullable(String.format("{\"Deployment\": {%s}}", ERROR));
        }
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String vmName = getVmName(provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName(), index);
        return Arrays.asList(new Resource(resourceType(), vmName + String.valueOf(new Date().getTime()), stack));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureTemplate azureTemplate = (AzureTemplate) stack.getTemplate();
        String vmName = buildResources.get(0).getResourceName();
        if (vmName.length() > MAX_NAME_LENGTH) {
            vmName = vmName.substring(vmName.length() - MAX_NAME_LENGTH, vmName.length());
        }
        Map<String, String> props = new HashMap<>();
        props.put(NAME, vmName);
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, provisionContextObject.getCommonName());
        return new AzureCloudServiceCreateRequest(props, provisionContextObject.getNewAzureClient((AzureCredential) stack.getCredential()),
                resources, buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_CLOUD_SERVICE;
    }

    public class AzureCloudServiceCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private AzureClient azureClient;
        private List<Resource> resources;

        public AzureCloudServiceCreateRequest(Map<String, String> props, AzureClient azureClient, List<Resource> resources, List<Resource> buildNames) {
            super(buildNames);
            this.props = props;
            this.azureClient = azureClient;
            this.resources = resources;
        }

        public Map<String, String> getProps() {
            return props;
        }

        public AzureClient getAzureClient() {
            return azureClient;
        }

        public List<Resource> getResources() {
            return resources;
        }
    }
}
