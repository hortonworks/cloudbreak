package com.sequenceiq.periscope.converter.db;

import jakarta.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;

public class UpdateFailedDetailsConverter implements AttributeConverter<UpdateFailedDetails, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedDetailsConverter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(UpdateFailedDetails attribute) {
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            LOGGER.error("Error writing UpdateFailedDetails to JSON", e);
        }
        return null;
    }

    @Override
    public UpdateFailedDetails convertToEntityAttribute(String dbData) {
        UpdateFailedDetails updateFailedDetails = null;
        try {
            updateFailedDetails = MAPPER.readValue(dbData, UpdateFailedDetails.class);
        } catch (Exception e) {
            LOGGER.error("Error while reading UpdateFailedDetails from JSON", e);
        }
        return updateFailedDetails;
    }
}
