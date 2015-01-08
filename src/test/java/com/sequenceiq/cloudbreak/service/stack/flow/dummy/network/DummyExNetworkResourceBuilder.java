package com.sequenceiq.cloudbreak.service.stack.flow.dummy.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

public class DummyExNetworkResourceBuilder
        implements ResourceBuilder<DummyProvisionContextObject, DummyDeleteContextObject, DummyDescribeContextObject, DummyStartStopContextObject> {

    @Override
    public Boolean create(CreateResourceRequest cRR) throws Exception {
        throw new BadRequestException("It's a test");
    }

    @Override
    public Boolean delete(Resource resource, DummyDeleteContextObject dummyDeleteContextObject) throws Exception {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, DummyDeleteContextObject dummyDeleteContextObject) throws Exception {
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, DummyDescribeContextObject dummyDescribeContextObject) throws Exception {
        return Optional.absent();
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.NETWORK_RESOURCE;
    }

    @Override
    public Boolean start(DummyStartStopContextObject dummyStartStopContextObject, Resource resource) {
        return true;
    }

    @Override
    public Boolean stop(DummyStartStopContextObject dummyStartStopContextObject, Resource resource) {
        return true;
    }

    @Override
    public List<Resource> buildNames(DummyProvisionContextObject po, int index, List<Resource> resources) {
        return Arrays.asList(new Resource(resourceType(), "network" + index, new Stack()));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(DummyProvisionContextObject po, List<Resource> res, List<Resource> buildNames, int index) throws Exception {
        return new DummyExNetworkCreateRequest(new ArrayList<Resource>());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_NETWORK;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

    public class DummyExNetworkCreateRequest extends CreateResourceRequest {

        public DummyExNetworkCreateRequest(List<Resource> buildableResources) {
            super(buildableResources);
        }
    }
}

