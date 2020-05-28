package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.AdjustmentType;

public class AdjustmentTypeConverter extends DefaultEnumConverter<AdjustmentType> {

    @Override
    public AdjustmentType getDefault() {
        return AdjustmentType.EXACT;
    }
}
