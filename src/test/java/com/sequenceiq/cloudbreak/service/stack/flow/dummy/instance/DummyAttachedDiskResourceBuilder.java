package com.sequenceiq.cloudbreak.service.stack.flow.dummy.instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

public class DummyAttachedDiskResourceBuilder
        implements ResourceBuilder<DummyProvisionContextObject, DummyDeleteContextObject, DummyStartStopContextObject> {

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, TemplateGroup templateGroup, String region) throws Exception {
        return true;
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
    public Boolean start(DummyStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean stop(DummyStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public List<Resource> buildResources(DummyProvisionContextObject provisionContextObject, int index, List<Resource> resources, TemplateGroup templateGroup) {
        return Arrays.asList(new Resource(resourceType(), "attacheddisk" + index, new Stack(), "master"));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(DummyProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, TemplateGroup templateGroup) throws Exception {
        return new DummyAttachedDiskCreateRequest(new ArrayList<Resource>());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCC_ATTACHED_DISK;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

    public class DummyAttachedDiskCreateRequest extends CreateResourceRequest {

        public DummyAttachedDiskCreateRequest(List<Resource> buildableResources) {
            super(buildableResources);
        }
    }
}
