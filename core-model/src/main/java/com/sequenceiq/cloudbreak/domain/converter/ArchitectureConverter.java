package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.model.Architecture;

public class ArchitectureConverter extends DefaultEnumConverter<Architecture> {

    @Override
    public Architecture convertToEntityAttribute(String attribute) {
        return Architecture.fromStringWithFallback(attribute);
    }

    @Override
    public Architecture getDefault() {
        return Architecture.UNKOWN;
    }
}
