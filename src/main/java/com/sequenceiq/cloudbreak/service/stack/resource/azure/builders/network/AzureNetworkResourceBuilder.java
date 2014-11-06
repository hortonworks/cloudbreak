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
    public List<Resource> create(AzureProvisionContextObject po, int index, List<Resource> resources) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        String name = stack.getName().replaceAll("\\s+", "") + String.valueOf(new Date().getTime());
        if (!po.getAzureClient().getVirtualNetworkConfiguration().toString().contains(name)) {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, name);
            props.put(AFFINITYGROUP, po.getCommonName());
            props.put(SUBNETNAME, name);
            props.put(ADDRESSPREFIX, "172.16.0.0/16");
            props.put(SUBNETADDRESSPREFIX, "172.16.0.0/24");
            HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) po.getAzureClient().createVirtualNetwork(props);
            String requestId = (String) po.getAzureClient().getRequestId(virtualNetworkResponse);
            waitUntilComplete(po.getAzureClient(), requestId);
        }
        return Arrays.asList(new Resource(ResourceType.AZURE_NETWORK, name, stack));
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
    public ResourceType resourceType() {
        return ResourceType.AZURE_NETWORK;
    }
}
