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
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(1)
public class AzureAffinityGroupResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, TemplateGroup templateGroup, String region) throws Exception {
        AzureAffinityGroupCreateRequest aCSCR = (AzureAffinityGroupCreateRequest) createResourceRequest;
        try {
            aCSCR.getAzureClient().getAffinityGroup(aCSCR.getName());
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                HttpResponseDecorator affinityResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createAffinityGroup(aCSCR.getProps());
                String requestId = (String) aCSCR.getAzureClient().getRequestId(affinityResponse);
                waitUntilComplete(aCSCR.getAzureClient(), requestId);
            } else if (ex instanceof HttpResponseException) {
                LOGGER.error(String.format("Error occurs on %s stack under the affinity group creation", aCSCR.getStackId()), ex);
                throw new InternalServerException(((HttpResponseException) ex).getResponse().toString());
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the affinity group creation", aCSCR.getStackId()), ex);
                throw new StackCreationFailureException(ex);
            }
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject azureDeleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, AzureDescribeContextObject describeContextObject, String region) throws Exception {
        try {
            Object affinityGroup = describeContextObject.getAzureClient().getAffinityGroup(resource.getResourceName());
            return Optional.fromNullable(affinityGroup.toString());
        } catch (Exception ex) {
            return Optional.fromNullable(String.format("{\"AffinityGroup\": {%s}}", ERROR));
        }
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources, TemplateGroup templateGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        return Arrays.asList(new Resource(resourceType(), provisionContextObject.getCommonName(), stack, templateGroup.getGroupName()));
    }

    @Override
    public AzureAffinityGroupCreateRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int i, TemplateGroup templateGroup) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureTemplate template = (AzureTemplate) templateGroup.getTemplate();
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        props.put(LOCATION, stack.getRegion());
        props.put(DESCRIPTION, template.getDescription());
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