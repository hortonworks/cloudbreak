package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.parameter.dto.DatabaseSetup;

public class DatabaseSetupConverter extends DefaultEnumConverter<DatabaseSetup> {

    @Override
    public DatabaseSetup getDefault() {
        return DatabaseSetup.PUBLIC;
    }
}
