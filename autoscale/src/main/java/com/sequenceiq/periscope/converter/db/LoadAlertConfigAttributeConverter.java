package com.sequenceiq.periscope.converter.db;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;

public class LoadAlertConfigAttributeConverter implements AttributeConverter<LoadAlertConfiguration, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadAlertConfigAttributeConverter.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(LoadAlertConfiguration loadAlertConfiguration) {
        try {
            return objectMapper.writeValueAsString(loadAlertConfiguration);
        } catch (Exception ex) {
            LOGGER.error("Error generating Load Alert Configuration Json", ex);
        }
        return null;
    }

    @Override
    public LoadAlertConfiguration convertToEntityAttribute(String dbConfig) {
        LoadAlertConfiguration loadAlertConfiguration = null;
        try {
            loadAlertConfiguration = objectMapper.readValue(dbConfig, LoadAlertConfiguration.class);
        } catch (Exception ex) {
            LOGGER.error("Error Read Load Alert Configuration", ex);
        }
        return loadAlertConfiguration;
    }
}
