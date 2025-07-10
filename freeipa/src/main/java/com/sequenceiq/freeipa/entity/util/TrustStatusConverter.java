package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.entity.TrustStatus;

public class TrustStatusConverter implements AttributeConverter<TrustStatus, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(TrustStatus stackStatus) {
        return stackStatus.name();
    }

    @Override
    public TrustStatus convertToEntityAttribute(String s) {
        try {
            return TrustStatus.valueOf(s);
        } catch (Exception e) {
            LOGGER.info("The TrustStatus value is not backward compatible: {}", s);
        }
        return TrustStatus.UNKNOWN;
    }
}
