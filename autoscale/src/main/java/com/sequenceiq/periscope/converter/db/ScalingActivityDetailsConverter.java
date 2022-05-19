package com.sequenceiq.periscope.converter.db;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.periscope.domain.ScalingActivityDetails;

public class ScalingActivityDetailsConverter implements AttributeConverter<ScalingActivityDetails, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingActivityDetailsConverter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ScalingActivityDetails attribute) {
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            LOGGER.error("Failed to convert ScalingActivityDetails to JSON", e);
        }
        return null;
    }

    @Override
    public ScalingActivityDetails convertToEntityAttribute(String dbData) {
        ScalingActivityDetails scalingActivityDetails = null;
        try {
            scalingActivityDetails = MAPPER.readValue(dbData, ScalingActivityDetails.class);
        } catch (Exception e) {
            LOGGER.error("Failed to read ScalingActivityDetails from JSON", e);
        }
        return scalingActivityDetails;
    }
}
