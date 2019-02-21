package com.sequenceiq.cloudbreak.cluster.util;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Component
public class ResourceAttributeUtil {

    public <T> Optional<T> getTypedAttributes(Resource resource, Class<T> attributeType) {
        Json attributes = resource.getAttributes();
        try {
            return Objects.nonNull(attributes.getValue()) ? Optional.ofNullable(attributes.get(attributeType)) : Optional.empty();
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to parse attributes to type: " + attributeType.getSimpleName(), e);
        }
    }

    public <T> void setTypedAttributes(Resource resource, T attributes) {
        try {
            resource.setAttributes(new Json(attributes));
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Failed to parse attributes from type: " + attributes.getClass().getSimpleName(), e);
        }
    }
}
