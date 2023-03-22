package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.model.AzureDatabaseType;

public class AzureDatabaseTypeConverter extends DefaultEnumConverter<AzureDatabaseType> {

    @Override
    public AzureDatabaseType getDefault() {
        return AzureDatabaseType.SINGLE_SERVER;
    }
}
