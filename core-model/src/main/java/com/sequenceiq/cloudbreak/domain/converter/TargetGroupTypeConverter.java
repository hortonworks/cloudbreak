package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.api.type.TargetGroupType;

public class TargetGroupTypeConverter extends DefaultEnumConverter<TargetGroupType> {

    @Override
    public TargetGroupType getDefault() {
        return TargetGroupType.KNOX;
    }
}
