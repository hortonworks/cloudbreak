package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class ResourceToResourceV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceToResourceV4ResponseConverter.class);

    public ResourceV4Response convertResourceToResourceV4Response(Resource resource) {
        LOGGER.debug("Converting resource to ResourceV4Response : {}", resource);
        ResourceV4Response resourceV4Response = new ResourceV4Response();
        resourceV4Response.setAttributes(null != resource.getAttributes() ? resource.getAttributes().getValue() : null);
        resourceV4Response.setId(resource.getId());
        resourceV4Response.setInstanceGroup(resource.getInstanceGroup());
        resourceV4Response.setResourceType(resource.getResourceType());
        resourceV4Response.setResourceStatus(resource.getResourceStatus());
        resourceV4Response.setResourceName(resource.getResourceName());
        resourceV4Response.setResourceReference(resource.getResourceReference());
        resourceV4Response.setResourceStack(resource.getStack().getId());
        resourceV4Response.setInstanceId(resource.getInstanceId());
        return resourceV4Response;
    }
}
