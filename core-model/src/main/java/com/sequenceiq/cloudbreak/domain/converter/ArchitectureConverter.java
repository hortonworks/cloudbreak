package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.cloud.model.Architecture;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

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
