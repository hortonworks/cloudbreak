package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.periscope.api.model.AdjustmentType;

public class AdjustmentTypeConverter extends DefaultEnumConverter<AdjustmentType> {

    @Override
    public AdjustmentType getDefault() {
        return AdjustmentType.EXACT;
    }
}
