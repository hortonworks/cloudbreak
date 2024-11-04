package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.model.SeLinux;

public class SeLinuxConverter extends DefaultEnumConverter<SeLinux> {

    @Override
    public SeLinux convertToEntityAttribute(String attribute) {
        return SeLinux.fromStringWithFallback(attribute);
    }

    @Override
    public SeLinux getDefault() {
        return SeLinux.PERMISSIVE;
    }
}
