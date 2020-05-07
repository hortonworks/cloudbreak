package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;

public class FeatureStateConverter implements AttributeConverter<FeatureState, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStateConverter.class);

    @Override
    public String convertToDatabaseColumn(FeatureState attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public FeatureState convertToEntityAttribute(String attribute) {
        try {
            return FeatureState.valueOf(attribute);
        } catch (Exception e) {
            LOGGER.info("The FeatureState value is not backward compatible: {}", attribute);
        }
        return FeatureState.RELEASED;
    }
}
