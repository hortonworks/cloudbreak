package com.sequenceiq.redbeams.repository.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.redbeams.api.model.common.Status;

public class StatusConverterTest extends DefaultEnumConverterBaseTest<Status> {

    @Override
    public Status getDefaultValue() {
        return Status.AVAILABLE;
    }

    @Override
    public AttributeConverter<Status, String> getVictim() {
        return new StatusConverter();
    }
}
