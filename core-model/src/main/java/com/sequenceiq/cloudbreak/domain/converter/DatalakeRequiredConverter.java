package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;

public class DatalakeRequiredConverter implements AttributeConverter<DatalakeRequired, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRequiredConverter.class);

    @Override
    public String convertToDatabaseColumn(DatalakeRequired attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DatalakeRequired convertToEntityAttribute(String attribute) {
        try {
            return DatalakeRequired.valueOf(attribute);
        } catch (Exception e) {
            LOGGER.info("The DatalakeRequired value is not backward compatible: {}", attribute);
        }
        return DatalakeRequired.OPTIONAL;
    }
}
