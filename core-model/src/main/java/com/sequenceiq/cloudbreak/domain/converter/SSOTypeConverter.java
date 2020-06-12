package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class SSOTypeConverter extends DefaultEnumConverter<SSOType> {

    @Override
    public SSOType getDefault() {
        return SSOType.NONE;
    }
}
