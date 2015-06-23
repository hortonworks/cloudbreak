package com.sequenceiq.cloudbreak.service.stack.flow.dummy.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;

public class DummyExNetworkResourceBuilder
        implements ResourceBuilder<DummyProvisionContextObject, DummyDeleteContextObject, DummyStartStopContextObject, UpdateContextObject> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DummyExNetworkResourceBuilder.class);

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        throw new BadRequestException("It's a test");
    }

    @Override
    public void update(UpdateContextObject updateContextObject) {
    }

    @Override
    public Boolean delete(Resource resource, DummyDeleteContextObject deleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, DummyDeleteContextObject deleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.NETWORK_RESOURCE;
    }

    @Override
    public void start(DummyStartStopContextObject startStopContextObject, Resource resource, String region) {
        LOGGER.debug("Network start requested - nothing to do.");
    }

    @Override
    public void stop(DummyStartStopContextObject startStopContextObject, Resource resource, String region) {
        LOGGER.debug("Network stop requested - nothing to do.");
    }

    @Override
    public List<Resource> buildResources(DummyProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        return Arrays.asList(new Resource(resourceType(), "network" + index, new Stack(), "master"));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(DummyProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        return new DummyExNetworkCreateRequest(new ArrayList<Resource>());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_NETWORK;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    public class DummyExNetworkCreateRequest extends CreateResourceRequest {

        public DummyExNetworkCreateRequest(List<Resource> buildableResources) {
            super(buildableResources);
        }
    }
}

