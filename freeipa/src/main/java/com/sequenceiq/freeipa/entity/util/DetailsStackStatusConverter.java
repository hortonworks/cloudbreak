package com.sequenceiq.freeipa.entity.util;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;

public class DetailsStackStatusConverter implements AttributeConverter<DetailedStackStatus, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetailsStackStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(DetailedStackStatus stackStatus) {
        return stackStatus.name();
    }

    @Override
    public DetailedStackStatus convertToEntityAttribute(String s) {
        try {
            return DetailedStackStatus.valueOf(s);
        } catch (Exception e) {
            LOGGER.info("The DetailedStackStatus value is not backward compatible: {}", s);
        }
        return DetailedStackStatus.UNKNOWN;
    }
}
