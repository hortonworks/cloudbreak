package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

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
import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(2)
public class AzureStorageAccountResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private AzureStackUtil azureStackUtil;
    @Autowired
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;
    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        AzureStorageAccountCreateRequest aCSCR = (AzureStorageAccountCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(aCSCR.stackId);
        try {
            aCSCR.getAzureClient().getStorageAccount(aCSCR.getName());
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException) {
                HttpResponseException httpResponseException = (HttpResponseException) ex;
                if (httpResponseException.getStatusCode() == NOT_FOUND) {
                    HttpResponseDecorator storageResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createStorageAccount(aCSCR.getProps());
                    AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aCSCR.getAzureClient(), stack, storageResponse);
                    azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                            POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
                } else {
                    LOGGER.error("Error creating storage: {}", aCSCR.getName(), httpResponseException);
                    throw new CloudConnectorException(httpResponseException.getResponse().toString());
                }
            } else {
                LOGGER.error("Error creating storage: {} for stack: {}", aCSCR.getName(), aCSCR.getStackId(), ex);
                throw new CloudConnectorException(ex);
            }
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject deleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), provisionContextObject.getAffinityGroupName(), stack, null));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(DESCRIPTION, "description");
        props.put(AFFINITYGROUP, provisionContextObject.getAffinityGroupName());
        AzureClient azureClient = azureStackUtil.createAzureClient(credential);
        return new AzureStorageAccountCreateRequest(buildResources.get(0).getResourceName(), provisionContextObject.getStackId(), props, azureClient,
                resources, buildResources);
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

        public AzureStorageAccountCreateRequest(String name, Long stackId, Map<String, String> props, AzureClient azureClient, List<Resource> resources,
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
