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
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
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
@Order(2)
public class AzureStorageAccountResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;

    @Override
    public Boolean create(CreateResourceRequest cRR) throws Exception {
        AzureStorageAccountCreateRequest aCSCR = (AzureStorageAccountCreateRequest) cRR;
        try {
            aCSCR.getAzureClient().getStorageAccount(aCSCR.getName());
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                HttpResponseDecorator storageResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createStorageAccount(aCSCR.getProps());
                String requestId = (String) aCSCR.getAzureClient().getRequestId(storageResponse);
                waitUntilComplete(aCSCR.getAzureClient(), requestId);
            } else if (ex instanceof HttpResponseException) {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", aCSCR.getStackId()), ex);
                throw new InternalServerException(((HttpResponseException) ex).getResponse().toString());
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", aCSCR.getStackId()), ex);
                throw new StackCreationFailureException(ex);
            }
        }
        return true;
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
    public List<String> buildNames(AzureProvisionContextObject po, int index, List<Resource> resources) {
        return Arrays.asList(po.getCommonName());
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject po, List<Resource> res, List<String> buildNames, int index) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildNames.get(0));
        props.put(DESCRIPTION, stack.getTemplate().getDescription());
        props.put(AFFINITYGROUP, po.getCommonName());
        AzureClient azureClient = po.getNewAzureClient(credential);
        return new AzureStorageAccountCreateRequest(buildNames.get(0), po.getStackId(), props, azureClient, res);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_STORAGE;
    }

    public class AzureStorageAccountCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private AzureClient azureClient;
        private String name;
        private Long stackId;
        private List<Resource> resources;

        public AzureStorageAccountCreateRequest(String name, Long stackId, Map<String, String> props, AzureClient azureClient, List<Resource> resources) {
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
