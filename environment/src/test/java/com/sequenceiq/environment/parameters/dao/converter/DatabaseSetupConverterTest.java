package com.sequenceiq.environment.parameters.dao.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameter.dto.DatabaseSetup;

public class DatabaseSetupConverterTest extends DefaultEnumConverterBaseTest<DatabaseSetup> {

    @Override
    public DatabaseSetup getDefaultValue() {
        return DatabaseSetup.PUBLIC;
    }

    @Override
    public AttributeConverter<DatabaseSetup, String> getVictim() {
        return new DatabaseSetupConverter();
    }
}