package com.sequenceiq.freeipa.entity.util;

import javax.persistence.AttributeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;

public class StackStatusConverter implements AttributeConverter<Status, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(Status stackStatus) {
        return stackStatus.name();
    }

    @Override
    public Status convertToEntityAttribute(String s) {
        try {
            return Status.valueOf(s);
        } catch (Exception e) {
            LOGGER.info("The StackStatus value is not backward compatible: {}", s);
        }
        return Status.UNKNOWN;
    }
}
