package com.sequenceiq.redbeams.converter.database;

import javax.inject.Inject;
import javax.persistence.AttributeConverter;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.service.crn.CrnService;

@Component
public class CrnConverter implements AttributeConverter<Crn, String> {

    @Inject
    private static CrnService crnServiceComponent;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    public void init(CrnService crnService) {
        crnServiceComponent = crnService;
    }

    @Override
    public String convertToDatabaseColumn(Crn attribute) {
        return attribute.getResource();
    }

    @Override
    public Crn convertToEntityAttribute(String dbData) {
        return crnServiceComponent.createDatabaseCrnFrom(dbData);
    }
}
