package com.sequenceiq.cloudbreak.converter;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.type.Tunnel;

public class TunnelConverter implements AttributeConverter<Tunnel, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TunnelConverter.class);

    @Override
    public String convertToDatabaseColumn(Tunnel attribute) {
        return attribute.name();
    }

    @Override
    public Tunnel convertToEntityAttribute(String dbData) {
        try {
            return Tunnel.valueOf(dbData);
        } catch (Exception e) {
            LOGGER.info("The Tunnel value is not backward compatible: {}", dbData);
        }
        return Tunnel.DIRECT;
    }
}
