package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(3)
public class AzureNetworkResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private AzureStackUtil azureStackUtil;
    @Autowired
    private AzureResourceStatusCheckerTask azureResourceStatusCheckerTask;
    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;


    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, InstanceGroup instanceGroup, String region) throws Exception {
        AzureNetworkCreateRequest aCSCR = (AzureNetworkCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(aCSCR.getStackId());
        if (!aCSCR.getAzureClient().getVirtualNetworkConfiguration().toString().contains(aCSCR.getName())) {
            HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createVirtualNetwork(aCSCR.getProps());
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aCSCR.getAzureClient(), virtualNetworkResponse, stack);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props;
        try {
            props = new HashMap<>();
            props.put(NAME, resource.getResourceName());
            AzureClient azureClient = azureStackUtil.createAzureClient(credential);
            HttpResponseDecorator deleteVirtualNetworkResult = (HttpResponseDecorator) azureClient.deleteVirtualNetwork(props);
            String requestId = (String) azureClient.getRequestId(deleteVirtualNetworkResult);
            boolean finished = azureClient.waitUntilComplete(requestId);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner(), stack);
        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage());
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources, InstanceGroup instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String s = stack.getName().replaceAll("\\s+", "") + String.valueOf(new Date().getTime());
        return Arrays.asList(new Resource(resourceType(), s, stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, InstanceGroup instanceGroup) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(AFFINITYGROUP, filterResourcesByType(resources, ResourceType.AZURE_AFFINITY_GROUP).get(0).getResourceName());
        props.put(SUBNETNAME, buildResources.get(0).getResourceName());
        props.put(ADDRESSPREFIX, "172.16.0.0/16");
        props.put(SUBNETADDRESSPREFIX, "172.16.0.0/24");
        AzureClient azureClient = azureStackUtil.createAzureClient(credential);
        return new AzureNetworkCreateRequest(buildResources.get(0).getResourceName(), provisionContextObject.getStackId(), props, azureClient,
                resources, buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_NETWORK;
    }

    public class AzureNetworkCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private AzureClient azureClient;
        private String name;
        private Long stackId;
        private List<Resource> resources;

        public AzureNetworkCreateRequest(String name, Long stackId, Map<String, String> props, AzureClient azureClient, List<Resource> resources,
                List<Resource> buildNames) {
            super(buildNames);
            this.name = name;
            this.stackId = stackId;
            this.props = props;
            this.azureClient = azureClient;
            this.resources = resources;
        }

        public String getName() {
            return name;
        }

        public Long getStackId() {
            return stackId;
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
