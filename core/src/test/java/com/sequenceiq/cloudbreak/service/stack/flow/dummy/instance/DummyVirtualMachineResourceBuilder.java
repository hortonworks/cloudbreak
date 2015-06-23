package com.sequenceiq.cloudbreak.service.stack.flow.dummy.instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
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

public class DummyVirtualMachineResourceBuilder
        implements ResourceBuilder<DummyProvisionContextObject, DummyDeleteContextObject, DummyStartStopContextObject, UpdateContextObject> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DummyVirtualMachineResourceBuilder.class);

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        return true;
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
        return ResourceBuilderType.INSTANCE_RESOURCE;
    }

    @Override
    public void start(DummyStartStopContextObject startStopContextObject, Resource resource, String region) {
        LOGGER.debug("VM stop requested - nothing to do.");
    }

    @Override
    public void stop(DummyStartStopContextObject startStopContextObject, Resource resource, String region) {
        LOGGER.debug("VM stop requested - nothing to do.");
    }

    @Override
    public List<Resource> buildResources(DummyProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        return Arrays.asList(new Resource(resourceType(), "virtualmachine" + index, new Stack(), "master"));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(DummyProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        return new DummyVirtualMachineCreateRequest(new ArrayList<Resource>());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_INSTANCE;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    public class DummyVirtualMachineCreateRequest extends CreateResourceRequest {

        public DummyVirtualMachineCreateRequest(List<Resource> buildableResources) {
            super(buildableResources);
        }
    }
}
