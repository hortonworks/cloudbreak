package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

public class SdxDatabaseAvailabilityTypeConverter extends DefaultEnumConverter<SdxDatabaseAvailabilityType> {

    @Override
    public SdxDatabaseAvailabilityType getDefault() {
        return SdxDatabaseAvailabilityType.NONE;
    }
}
