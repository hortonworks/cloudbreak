package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public interface ResourceBuilder<P extends ProvisionContextObject, D extends DeleteContextObject, DCO extends DescribeContextObject,
        SSCO extends StartStopContextObject> {

    Boolean create(CreateResourceRequest createResourceRequest) throws Exception;

    Boolean delete(Resource resource, D deleteContextObject) throws Exception;

    Boolean rollback(Resource resource, D deleteContextObject) throws Exception;

    Optional<String> describe(Resource resource, DCO describeContextObject) throws Exception;

    ResourceBuilderType resourceBuilderType();

    Boolean start(SSCO startStopContextObject, Resource resource);

    Boolean stop(SSCO startStopContextObject, Resource resource);

    List<Resource> buildResources(P provisionContextObject, int index, List<Resource> resources);

    CreateResourceRequest buildCreateRequest(P provisionContextObject, List<Resource> resources, List<Resource> buildResources, int index) throws Exception;

    ResourceType resourceType();

    CloudPlatform cloudPlatform();
}
