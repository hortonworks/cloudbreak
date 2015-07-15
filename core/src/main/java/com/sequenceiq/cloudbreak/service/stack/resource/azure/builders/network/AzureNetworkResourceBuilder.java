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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.network.Port;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceNameService;
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

    @Inject
    private StackRepository stackRepository;
    @Inject
    private AzureStackUtil azureStackUtil;
    @Inject
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Inject
    @Named("AzureResourceNameService")
    private ResourceNameService resourceNameService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        AzureNetworkCreateRequest request = (AzureNetworkCreateRequest) createResourceRequest;
        try {
            Stack stack = stackRepository.findByIdLazy(request.getStackId());
            AzureClient azureClient = request.getAzureClient();
            Map<String, String> props = request.getProps();
            if (!azureClient.getVirtualNetworks().toString().contains(request.getName())) {
                HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
                AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                        azureClient, ResourceType.AZURE_NETWORK, props.get(NAME), stack, virtualNetworkResponse);
                azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
            }
        } catch (Exception ex) {
            throw checkException(ex);
        }
        return true;
    }

    @Override
    public void update(AzureUpdateContextObject updateContextObject) {
        Stack stack = updateContextObject.getStack();
        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        Stack stackWithSecurityGroup = stackRepository.findByIdWithSecurityGroup(stack.getId());
        List<Port> ports = NetworkUtils.getPorts(Optional.fromNullable(stackWithSecurityGroup));
        Resource network = stack.getResourceByType(ResourceType.AZURE_NETWORK);
        List<HttpResponseDecorator> responses = new ArrayList<>();
        List<String> resourceNames = new ArrayList<>();
        try {
            for (Resource resource : stack.getResourcesByType(ResourceType.AZURE_VIRTUAL_MACHINE)) {
                Map<String, Object> props = new HashMap<>();
                props.put(NAME, resource.getResourceName());
                props.put(SUBNETNAME, network.getResourceName());
                props.put(VIRTUAL_NETWORK_IP_ADDRESS, instanceMetaDataRepository.findByInstanceId(stack.getId(), resource.getResourceName()).getPrivateIp());
                props.put(PORTS, ports);
                HttpResponseDecorator response = (HttpResponseDecorator) azureClient.updateEndpoints(props);
                responses.add(response);
                resourceNames.add(resource.getResourceName());
            }
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                    azureClient, ResourceType.AZURE_NETWORK, StringUtils.join(resourceNames, ','), stack, responses);
            PollingResult pollingResult =
                    azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                            POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (isExited(pollingResult)) {
                LOGGER.warn("Polling result: {}. Failed to update azure network resource.");
                throw new AzureResourceException(String.format("Failed to update azure network resource; polling result: '%s', stack id", pollingResult,
                        stack.getId()));
            }
        } catch (Exception e) {
            LOGGER.warn("Exception during azure network resource update.");
            throw new AzureResourceException(String.format("Failed to update azure network resource; stack id:", stack.getId()));
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
            throw new AzureResourceException(ex.getMessage());
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String s = resourceNameService.resourceName(resourceType(), stack.getName(), new Date());
        return asList(new Resource(resourceType(), s, stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureNetwork network = (AzureNetwork) stack.getNetwork();
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(AFFINITYGROUP, filterResourcesByType(resources, ResourceType.AZURE_AFFINITY_GROUP).get(0).getResourceName());
        props.put(SUBNETNAME, buildResources.get(0).getResourceName());
        props.put(ADDRESSPREFIX, network.getAddressPrefixCIDR());
        props.put(SUBNETADDRESSPREFIX, network.getSubnetCIDR());
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
