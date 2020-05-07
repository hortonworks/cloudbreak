package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;

public class DetailedDBStackStatusConverter implements AttributeConverter<DetailedDBStackStatus, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetailedDBStackStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(DetailedDBStackStatus attribute) {
        return attribute.name();
    }

    @Override
    public DetailedDBStackStatus convertToEntityAttribute(String dbData) {
        try {
            return DetailedDBStackStatus.valueOf(dbData);
        } catch (Exception e) {
            LOGGER.info("The DetailedDBStackStatus value is not backward compatible: {}", dbData);
        }
        return DetailedDBStackStatus.AVAILABLE;
    }
}
