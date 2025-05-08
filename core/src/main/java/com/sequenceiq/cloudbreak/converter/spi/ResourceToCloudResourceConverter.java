package com.sequenceiq.cloudbreak.converter.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class ResourceToCloudResourceConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceToCloudResourceConverter.class);

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
                .withAvailabilityZone(resource.getAvailabilityZone())
                .withGroup(resource.getInstanceGroup())
                .withInstanceId(resource.getInstanceId())
                .withPrivateId(resource.getPrivateId())
                .withParameters(paramsMap)
                .build();
    }
}
