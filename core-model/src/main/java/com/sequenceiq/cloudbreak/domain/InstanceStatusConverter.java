package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;

public class InstanceStatusConverter implements AttributeConverter<InstanceStatus, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(InstanceStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public InstanceStatus convertToEntityAttribute(String dbData) {
        try {
            return dbData != null ? InstanceStatus.valueOf(dbData) : null;
        } catch (Exception e) {
            LOGGER.info("The InstanceStatus value is not backward compatible: {}", dbData);
        }
        return InstanceStatus.STARTED;
    }
}
