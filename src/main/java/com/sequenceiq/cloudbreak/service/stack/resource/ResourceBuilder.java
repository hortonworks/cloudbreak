package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;

public interface ResourceBuilder<P extends ProvisionContextObject, D extends DeleteContextObject, DCO extends DescribeContextObject,
        SSCO extends StartStopContextObject> {

    Boolean create(CreateResourceRequest createResourceRequest, TemplateGroup templateGroup, String region) throws Exception;

    Boolean delete(Resource resource, D d, String region) throws Exception;

    Boolean rollback(Resource resource, D d, String region) throws Exception;

    ResourceBuilderType resourceBuilderType();

    Boolean start(SSCO startStopContextObject, Resource resource, String region);

    Boolean stop(SSCO startStopContextObject, Resource resource, String region);

    List<Resource> buildResources(P provisionContextObject, int index, List<Resource> resources, TemplateGroup templateGroup);

    CreateResourceRequest buildCreateRequest(P provisionContextObject, List<Resource> resources, List<Resource> buildResources,
            int index, TemplateGroup templateGroup) throws Exception;

    ResourceType resourceType();

    CloudPlatform cloudPlatform();
}
