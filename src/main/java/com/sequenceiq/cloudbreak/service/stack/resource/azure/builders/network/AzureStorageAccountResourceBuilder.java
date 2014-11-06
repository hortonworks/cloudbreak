package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
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
@Order(2)
public class AzureStorageAccountResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;

    @Override
    public List<Resource> create(AzureProvisionContextObject po, int index, List<Resource> resources) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        AzureTemplate template = (AzureTemplate) stack.getTemplate();
        try {
            po.getAzureClient().getStorageAccount(po.getCommonName());
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> props = new HashMap<>();
                props.put(NAME, po.getCommonName());
                props.put(DESCRIPTION, template.getDescription());
                props.put(AFFINITYGROUP, po.getCommonName());
                HttpResponseDecorator storageResponse = (HttpResponseDecorator) po.getAzureClient().createStorageAccount(props);
                String requestId = (String) po.getAzureClient().getRequestId(storageResponse);
                waitUntilComplete(po.getAzureClient(), requestId);
            } else if (ex instanceof HttpResponseException) {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", stack.getId()), ex);
                throw new InternalServerException(((HttpResponseException) ex).getResponse().toString());
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", stack.getId()), ex);
                throw new StackCreationFailureException(ex);
            }
        }
        return Arrays.asList(new Resource(ResourceType.AZURE_STORAGE, po.getCommonName(), stack));
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject aDCO) throws Exception {
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, AzureDescribeContextObject aDCO) throws Exception {
        try {
            Object storageAccount = aDCO.getAzureClient().getStorageAccount(resource.getResourceName());
            return Optional.fromNullable(storageAccount.toString());
        } catch (Exception ex) {
            return Optional.fromNullable(String.format("{\"StorageService\": {%s}}", ERROR));
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_STORAGE;
    }
}
