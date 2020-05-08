package com.sequenceiq.cloudbreak.converter;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DefaultEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEnumConverter.class);

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public E convertToEntityAttribute(String attribute) {
        try {
            if (attribute == null) {
                return null;
            }
            return (E) Enum.valueOf(getDefault().getClass(), attribute);
        } catch (Exception e) {
            LOGGER.info("The {} value is not backward compatible: {}", getDefault().getClass().getSimpleName(), attribute);
        }
        return getDefault();
    }

    public abstract E getDefault();

}
