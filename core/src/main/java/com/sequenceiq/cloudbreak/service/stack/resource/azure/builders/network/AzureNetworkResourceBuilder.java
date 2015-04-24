package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.PORTS;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.VIRTUAL_NETWORK_IP_ADDRESS;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.network.NetworkConfig;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.network.Port;
import com.sequenceiq.cloudbreak.service.stack.connector.UpdateFailedException;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureUpdateContextObject;

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
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;
    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        AzureNetworkCreateRequest request = (AzureNetworkCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(request.getStackId());
        if (!request.getAzureClient().getVirtualNetworkConfiguration().toString().contains(request.getName())) {
            HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) request.getAzureClient().createVirtualNetwork(request.getProps());
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(request.getAzureClient(), stack, virtualNetworkResponse);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        }
        return true;
    }

    @Override
    public void update(AzureUpdateContextObject updateContextObject) throws UpdateFailedException {
        Stack stack = updateContextObject.getStack();
        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        List<Port> ports = NetworkUtils.getPorts(stack);
        Resource network = stack.getResourceByType(ResourceType.AZURE_NETWORK);
        List<HttpResponseDecorator> responses = new ArrayList<>();
        try {
            for (Resource resource : stack.getResourcesByType(ResourceType.AZURE_VIRTUAL_MACHINE)) {
                Map<String, Object> props = new HashMap<>();
                props.put(NAME, resource.getResourceName());
                props.put(SUBNETNAME, network.getResourceName());
                props.put(VIRTUAL_NETWORK_IP_ADDRESS, instanceMetaDataRepository.findByInstanceId(resource.getResourceName()).getPrivateIp());
                props.put(PORTS, ports);
                HttpResponseDecorator response = (HttpResponseDecorator) azureClient.updateEndpoints(props);
                responses.add(response);
            }
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(azureClient, stack, responses);
            PollingResult pollingResult =
                    azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                            POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (isExited(pollingResult)) {
                throw new UpdateFailedException(new IllegalStateException());
            }
        } catch (Exception e) {
            throw new UpdateFailedException(e);
        }
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
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String s = stack.getName().replaceAll("\\s+", "") + String.valueOf(new Date().getTime());
        return asList(new Resource(resourceType(), s, stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(AFFINITYGROUP, filterResourcesByType(resources, ResourceType.AZURE_AFFINITY_GROUP).get(0).getResourceName());
        props.put(SUBNETNAME, buildResources.get(0).getResourceName());
        props.put(ADDRESSPREFIX, NetworkConfig.SUBNET_8);
        props.put(SUBNETADDRESSPREFIX, NetworkConfig.SUBNET_16);
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
