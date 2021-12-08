package com.sequenceiq.flow.converter;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.domain.ClassValue;

public class ClassValueConverter implements AttributeConverter<ClassValue, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassValueConverter.class);

    @Override
    public String convertToDatabaseColumn(ClassValue attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getName();
    }

    @Override
    public ClassValue convertToEntityAttribute(String dbData) {
        if (StringUtils.isEmpty(dbData)) {
            return null;
        }
        try {
            return ClassValue.of(dbData);
        } catch (ClassNotFoundException e) {
            LOGGER.info("Unknown class type {}.", dbData);
            return ClassValue.ofUnknown(dbData);
        }
    }
}
