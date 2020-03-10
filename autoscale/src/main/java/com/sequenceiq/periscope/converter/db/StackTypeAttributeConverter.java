package com.sequenceiq.periscope.converter.db;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

public class StackTypeAttributeConverter implements AttributeConverter<StackType, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTypeAttributeConverter.class);

    @Override
    public String convertToDatabaseColumn(StackType attribute) {
        return attribute.name();
    }

    @Override
    public StackType convertToEntityAttribute(String dbData) {
        try {
            return StackType.valueOf(dbData);
        } catch (Exception e) {
            LOGGER.info("The StackType value is not backward compatible: {}", dbData);
        }
        return StackType.DATALAKE;
    }
}
