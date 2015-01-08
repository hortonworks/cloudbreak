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

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(3)
public class AzureNetworkResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;

    @Override
    public Boolean create(CreateResourceRequest cRR) throws Exception {
        AzureNetworkCreateRequest aCSCR = (AzureNetworkCreateRequest) cRR;
        if (!aCSCR.getAzureClient().getVirtualNetworkConfiguration().toString().contains(aCSCR.getName())) {
            HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createVirtualNetwork(aCSCR.getProps());
            String requestId = (String) aCSCR.getAzureClient().getRequestId(virtualNetworkResponse);
            waitUntilComplete(aCSCR.getAzureClient(), requestId);
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject aDCO) throws Exception {
        Stack stack = stackRepository.findById(aDCO.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props;
        try {
            props = new HashMap<>();
            props.put(NAME, resource.getResourceName());
            AzureClient azureClient = aDCO.getNewAzureClient(credential);
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
    public Optional<String> describe(Resource resource, AzureDescribeContextObject azureDescribeContextObject) throws Exception {
        return Optional.absent();
    }

    @Override
    public List<Resource> buildNames(AzureProvisionContextObject po, int index, List<Resource> resources) {
        Stack stack = stackRepository.findById(po.getStackId());
        String s = stack.getName().replaceAll("\\s+", "") + String.valueOf(new Date().getTime());
        return Arrays.asList(new Resource(resourceType(), s, stack));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject po, List<Resource> res, List<Resource> buildNames, int index) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildNames.get(0).getResourceName());
        props.put(AFFINITYGROUP, filterResourcesByType(res, ResourceType.AZURE_AFFINITY_GROUP).get(0).getResourceName());
        props.put(SUBNETNAME, buildNames.get(0).getResourceName());
        props.put(ADDRESSPREFIX, "172.16.0.0/16");
        props.put(SUBNETADDRESSPREFIX, "172.16.0.0/24");
        AzureClient azureClient = po.getNewAzureClient(credential);
        return new AzureNetworkCreateRequest(buildNames.get(0).getResourceName(), po.getStackId(), props, azureClient, res, buildNames);
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
