package com.sequenceiq.periscope.converter.db;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class StackTypeAttributeConverter extends DefaultEnumConverter<StackType> {

    @Override
    public StackType getDefault() {
        return StackType.TEMPLATE;
    }
}
