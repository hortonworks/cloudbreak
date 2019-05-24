package com.sequenceiq.redbeams.converter.database;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.AttributeConverter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.service.crn.CrnService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class CrnConverter implements AttributeConverter<Crn, String> {

    private static CrnService crnService;

    @Inject
    private CrnService crnServiceComponent;

    @PostConstruct
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void init() {
        crnService = crnServiceComponent;
    }

    @Override
    public String convertToDatabaseColumn(Crn attribute) {
        return attribute.getResource();
    }

    @Override
    public Crn convertToEntityAttribute(String dbData) {
        return crnService.createDatabaseCrnFrom(dbData);
    }
}
