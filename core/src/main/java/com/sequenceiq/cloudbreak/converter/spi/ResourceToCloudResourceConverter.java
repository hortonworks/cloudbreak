package com.sequenceiq.cloudbreak.converter.spi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
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
        enrichWithRawAttributeEntries(resource, paramsMap);

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

    private void enrichWithRawAttributeEntries(Resource resource, Map<String, Object> paramsMap) {
        Json jsonAttributes = resource.getAttributes();
        if (jsonAttributes != null && Objects.nonNull(jsonAttributes.getValue())) {
            try {
                Map<String, Object> rawMap = jsonAttributes.get(Map.class);
                rawMap.forEach((key, value) -> {
                    if (!CloudResource.ATTRIBUTE_TYPE.equals(key)) {
                        paramsMap.putIfAbsent(key, value);
                    }
                });
            } catch (IOException e) {
                LOGGER.warn("Failed to read raw attributes map for resource {}", resource.getResourceName(), e);
            }
        }
    }
}
