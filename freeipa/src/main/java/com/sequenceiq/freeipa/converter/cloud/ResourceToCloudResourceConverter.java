package com.sequenceiq.freeipa.converter.cloud;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;

@Component
public class ResourceToCloudResourceConverter {

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    public CloudResource convert(Resource resource) {
        Optional<Object> attributes = resourceAttributeUtil.getTypedAttributes(resource);

        Map<String, Object> paramsMap = new HashMap<>();
        attributes.ifPresent(attr -> paramsMap.put(CloudResource.ATTRIBUTES, attr));

        return new Builder()
                .type(resource.getResourceType())
                .name(resource.getResourceName())
                .reference(resource.getResourceReference())
                .status(resource.getResourceStatus())
                .group(resource.getInstanceGroup())
                .instanceId(resource.getInstanceId())
                .params(paramsMap)
                .build();
    }
}
