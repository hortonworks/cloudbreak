package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;

public class ClusterTemplateV4TypeConverter implements AttributeConverter<ClusterTemplateV4Type, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4TypeConverter.class);

    @Override
    public String convertToDatabaseColumn(ClusterTemplateV4Type attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ClusterTemplateV4Type convertToEntityAttribute(String attribute) {
        try {
            return ClusterTemplateV4Type.valueOf(attribute);
        } catch (Exception e) {
            LOGGER.info("The ClusterTemplateType value is not backward compatible: {}", attribute);
        }
        return ClusterTemplateV4Type.OTHER;
    }
}
