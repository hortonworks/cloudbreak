package com.sequenceiq.cloudbreak.cluster.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class ResourceAttributeUtil {

    private static final Logger LOGGER = getLogger(ResourceAttributeUtil.class);

    public <T> Optional<T> getTypedAttributes(Resource resource, Class<T> attributeType) {
        Json attributes = resource.getAttributes();
        try {
            return Objects.nonNull(attributes.getValue()) ? Optional.ofNullable(attributes.get(attributeType)) : Optional.empty();
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to parse attributes to type: " + attributeType.getSimpleName(), e);
        }
    }

    public <T> Optional<T> getTypedAttributes(Resource resource) {
        Json attributes = resource.getAttributes();
        Optional<T> ret = Optional.empty();
        try {
            if (Objects.nonNull(attributes.getValue())) {
                Map<String, Object> map = attributes.get(Map.class);
                String clss = map.getOrDefault(CloudResource.ATTRIBUTE_TYPE, VolumeSetAttributes.class.getCanonicalName()).toString();
                Class<T> attributeType = (Class<T>) Class.forName(clss);
                ret = Optional.ofNullable(attributes.get(attributeType));
            }
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to parse attributes to type: " + attributes, e);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Cannot parse class: {}", e.getMessage());
        }
        return ret;
    }

    public <T> void setTypedAttributes(Resource resource, T attributes) {
        try {
            resource.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            throw new CloudbreakServiceException("Failed to parse attributes from type: " + attributes.getClass().getSimpleName(), e);
        }
    }
}
