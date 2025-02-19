package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.model.Architecture;

public class ArchitectureConverter extends DefaultEnumConverter<Architecture> {

    @Override
    public Architecture convertToEntityAttribute(String attribute) {
        return Architecture.fromStringWithFallback(attribute);
    }

    @Override
    public Architecture getDefault() {
        return Architecture.UNKNOWN;
    }
}
