package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public interface ResourceBuilder<P extends ProvisionContextObject,
        D extends DeleteContextObject, SSCO extends StartStopContextObject, U extends UpdateContextObject> {

    Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception;

    void update(U updateContextObject);

    Boolean delete(Resource resource, D d, String region) throws Exception;

    Boolean rollback(Resource resource, D d, String region) throws Exception;

    ResourceBuilderType resourceBuilderType();

    void start(SSCO startStopContextObject, Resource resource, String region);

    void stop(SSCO startStopContextObject, Resource resource, String region);

    List<Resource> buildResources(P provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup);

    CreateResourceRequest buildCreateRequest(P provisionContextObject, List<Resource> resources, List<Resource> buildResources,
            int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception;

    ResourceType resourceType();

    CloudPlatform cloudPlatform();
}
