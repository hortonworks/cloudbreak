package com.sequenceiq.freeipa.converter.cloud;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
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

        return CloudResource.builder()
                .withType(resource.getResourceType())
                .withName(resource.getResourceName())
                .withReference(resource.getResourceReference())
                .withStatus(resource.getResourceStatus())
                .withGroup(resource.getInstanceGroup())
                .withInstanceId(resource.getInstanceId())
                .withParameters(paramsMap)
                .withAvailabilityZone(resource.getAvailabilityZone())
                .build();
    }
}
