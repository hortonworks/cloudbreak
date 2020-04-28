package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ScalingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;

public class ScalingModeConverter implements AttributeConverter<ScalingMode, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingModeConverter.class);

    @Override
    public String convertToDatabaseColumn(ScalingMode scalingMode) {
        return scalingMode.name();
    }

    @Override
    public ScalingMode convertToEntityAttribute(String dbData) {
        try {
            return ScalingMode.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            LOGGER.info("Scaling mode {} is no longer supported, fallback to UNSPECIFIED.", dbData);
        }
        return ScalingMode.UNSPECIFIED;
    }
}
