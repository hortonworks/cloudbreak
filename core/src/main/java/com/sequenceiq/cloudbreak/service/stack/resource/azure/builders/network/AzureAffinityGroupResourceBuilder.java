package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
@Order(1)
public class AzureAffinityGroupResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private AzureStackUtil azureStackUtil;
    @Inject
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;
    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        AzureAffinityGroupCreateRequest aCSCR = (AzureAffinityGroupCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(aCSCR.stackId);
        AzureClient azureClient = aCSCR.getAzureClient();
        Map<String, String> props = aCSCR.getProps();
        try {
            LOGGER.debug("Checking for affinity group: {}", aCSCR.getName());
            azureClient.getAffinityGroup(aCSCR.getName());
        } catch (Exception ex) {
            if (ex instanceof HttpResponseException) {
                HttpResponseException httpResponseException = (HttpResponseException) ex;
                if (httpResponseException.getStatusCode() == NOT_FOUND) {
                    LOGGER.debug("Affinity group not found; creating new  affinity group: {}", aCSCR.getName());
                    HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
                    AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                            azureClient, ResourceType.AZURE_AFFINITY_GROUP, props.get(NAME), stack, affinityResponse);
                    azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                            POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
                } else {
                    LOGGER.error("Error creating affinity group: {}", aCSCR.getName(), httpResponseException);
                    throw new CloudConnectorException(httpResponseException.getResponse().toString());
                }
            } else {
                LOGGER.error("Error creating affinity group: {} for stack: {}", aCSCR.getName(), aCSCR.getStackId(), ex);
                throw new CloudConnectorException(ex);
            }
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject azureDeleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), provisionContextObject.getAffinityGroupName(), stack, null));
    }

    @Override
    public AzureAffinityGroupCreateRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int i, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(LOCATION, stack.getRegion());
        props.put(DESCRIPTION, "description");
        return new AzureAffinityGroupCreateRequest(buildResources.get(0).getResourceName(), provisionContextObject.getStackId(), props,
                azureStackUtil.createAzureClient(azureCredential), resources, buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_AFFINITY_GROUP;
    }

    public class AzureAffinityGroupCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private AzureClient azureClient;
        private String name;
        private Long stackId;
        private List<Resource> resources;

        public AzureAffinityGroupCreateRequest(String name, Long stackId, Map<String, String> props, AzureClient azureClient, List<Resource> resources,
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