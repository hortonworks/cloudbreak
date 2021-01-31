package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;

public class VolumeUsageTypeConverter extends DefaultEnumConverter<VolumeUsageType> {
    @Override
    public VolumeUsageType getDefault() {
        return VolumeUsageType.GENERAL;
    }
}
