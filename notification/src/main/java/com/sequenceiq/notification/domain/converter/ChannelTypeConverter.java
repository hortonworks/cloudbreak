package com.sequenceiq.notification.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.notification.domain.ChannelType;

public class ChannelTypeConverter extends DefaultEnumConverter<ChannelType> {

    @Override
    public ChannelType getDefault() {
        return ChannelType.EMAIL;
    }
}
