package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

public class ResourceStatusConverter implements AttributeConverter<ResourceStatus, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(ResourceStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ResourceStatus convertToEntityAttribute(String attribute) {
        try {
            return ResourceStatus.valueOf(attribute);
        } catch (Exception e) {
            LOGGER.info("The ResourceStatus value is not backward compatible: {}", attribute);
        }
        return ResourceStatus.USER_MANAGED;
    }
}
