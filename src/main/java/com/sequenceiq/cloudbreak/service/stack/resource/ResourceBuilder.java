package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

public interface ResourceBuilder<P extends ProvisionContextObject, D extends DeleteContextObject, SSCO extends StartStopContextObject> {

    Boolean create(CreateResourceRequest createResourceRequest, InstanceGroup instanceGroup, String region) throws Exception;

    Boolean delete(Resource resource, D d, String region) throws Exception;

    Boolean rollback(Resource resource, D d, String region) throws Exception;

    ResourceBuilderType resourceBuilderType();

    Boolean start(SSCO startStopContextObject, Resource resource, String region);

    Boolean stop(SSCO startStopContextObject, Resource resource, String region);

    List<Resource> buildResources(P provisionContextObject, int index, List<Resource> resources, InstanceGroup instanceGroup);

    CreateResourceRequest buildCreateRequest(P provisionContextObject, List<Resource> resources, List<Resource> buildResources,
            int index, InstanceGroup instanceGroup) throws Exception;

    ResourceType resourceType();

    CloudPlatform cloudPlatform();
}
