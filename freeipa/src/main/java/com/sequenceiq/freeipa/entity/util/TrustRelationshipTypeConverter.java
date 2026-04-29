package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class TrustRelationshipTypeConverter implements AttributeConverter<TrustRelationshipType, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustRelationshipTypeConverter.class);

    @Override
    public String convertToDatabaseColumn(TrustRelationshipType trustRelationshipType) {
        return trustRelationshipType.name();
    }

    @Override
    public TrustRelationshipType convertToEntityAttribute(String s) {
        try {
            return TrustRelationshipType.valueOf(s);
        } catch (Exception e) {
            LOGGER.info("The TrustRelationshipType value is not backward compatible: {}", s);
        }
        return TrustRelationshipType.UNKNOWN;
    }
}


