package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureCloudServiceDeleteTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureCloudServiceDeleteTaskContext;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;

@Component
@Order(1)
public class AzureCloudServiceResourceBuilder extends AzureSimpleInstanceResourceBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DISK_MAX_ATTEMPTS = 100;

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private AzureCloudServiceDeleteTask azureCloudServiceDeleteTask;
    @Autowired
    private PollingService<AzureCloudServiceDeleteTaskContext> azureCloudServiceRemoveReadyPollerObjectPollingService;
    @Autowired
    private AzureStackUtil azureStackUtil;
    @Autowired
    private AzureResourceStatusCheckerTask azureResourceStatusCheckerTask;
    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;


    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, TemplateGroup templateGroup, String region) throws Exception {
        AzureCloudServiceCreateRequest aCSCR = (AzureCloudServiceCreateRequest) createResourceRequest;
        HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createCloudService(aCSCR.getProps());
        AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aCSCR.getAzureClient(), cloudServiceResponse, aCSCR.getStack());
        azureResourcePollerObjectPollingService.pollWithTimeout(azureResourceStatusCheckerTask, azureResourcePollerObject,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureCloudServiceDeleteTaskContext azureCloudServiceDeleteTaskContext =
                new AzureCloudServiceDeleteTaskContext(deleteContextObject.getCommonName(), resource.getResourceName(),
                        stack, azureStackUtil.createAzureClient(credential));
        azureCloudServiceRemoveReadyPollerObjectPollingService
                .pollWithTimeout(azureCloudServiceDeleteTask, azureCloudServiceDeleteTaskContext, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources, TemplateGroup templateGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String vmName = getVmName(provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName(), index);
        return Arrays.asList(new Resource(resourceType(), vmName + String.valueOf(new Date().getTime()), stack, templateGroup.getGroupName()));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, TemplateGroup templateGroup) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureTemplate azureTemplate = (AzureTemplate) templateGroup.getTemplate();
        String vmName = buildResources.get(0).getResourceName();
        if (vmName.length() > MAX_NAME_LENGTH) {
            vmName = vmName.substring(vmName.length() - MAX_NAME_LENGTH, vmName.length());
        }
        Map<String, String> props = new HashMap<>();
        props.put(NAME, vmName);
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, provisionContextObject.getCommonName());
        return new AzureCloudServiceCreateRequest(props, azureStackUtil.createAzureClient((AzureCredential) stack.getCredential()),
                resources, buildResources, stack);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_CLOUD_SERVICE;
    }

    public class AzureCloudServiceCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private AzureClient azureClient;
        private List<Resource> resources;
        private Stack stack;

        public AzureCloudServiceCreateRequest(Map<String, String> props, AzureClient azureClient, List<Resource> resources, List<Resource> buildNames,
                Stack stack) {
            super(buildNames);
            this.props = props;
            this.azureClient = azureClient;
            this.resources = resources;
            this.stack = stack;
        }

        public Stack getStack() {
            return stack;
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
