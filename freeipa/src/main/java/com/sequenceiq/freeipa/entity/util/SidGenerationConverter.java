package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.dto.SidGeneration;

public class SidGenerationConverter extends DefaultEnumConverter<SidGeneration> {
    @Override
    public SidGeneration getDefault() {
        return SidGeneration.DISABLED;
    }
}
