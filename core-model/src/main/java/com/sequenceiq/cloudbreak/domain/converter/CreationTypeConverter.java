package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.CreationType;

public class CreationTypeConverter extends DefaultEnumConverter<CreationType> {
    @Override
    public CreationType getDefault() {
        return CreationType.USER;
    }
}
