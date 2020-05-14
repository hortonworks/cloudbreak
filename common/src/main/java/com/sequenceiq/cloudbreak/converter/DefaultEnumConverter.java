package com.sequenceiq.cloudbreak.converter;

import java.util.Optional;

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
        Optional<E> convertedEnum = tryConvertUnknownField(attribute);
        return convertedEnum.orElse(getDefault());
    }

    public abstract E getDefault();

    protected Optional<E> tryConvertUnknownField(String attribute) {
        return Optional.empty();
    }
}
