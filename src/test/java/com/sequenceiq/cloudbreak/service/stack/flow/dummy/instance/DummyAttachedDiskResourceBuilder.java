package com.sequenceiq.cloudbreak.service.stack.flow.dummy.instance;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.flow.dummy.DummyStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

public class DummyAttachedDiskResourceBuilder
        implements ResourceBuilder<DummyProvisionContextObject, DummyDeleteContextObject, DummyDescribeContextObject, DummyStartStopContextObject> {

    @Override
    public Boolean create(CreateResourceRequest cRR) throws Exception {
        return true;
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
        return ResourceBuilderType.INSTANCE_RESOURCE;
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
    public List<String> buildNames(DummyProvisionContextObject po, int index, List<Resource> resources) {
        return Arrays.asList("attacheddisk" + index);
    }

    @Override
    public CreateResourceRequest buildCreateRequest(DummyProvisionContextObject po, List<Resource> res, List<String> buildNames, int index) throws Exception {
        return new DummyAttachedDiskCreateRequest();
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

    }
}
