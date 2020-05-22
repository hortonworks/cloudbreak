package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ComponentTypeConverter extends DefaultEnumConverter<ComponentType> {

    @Override
    public ComponentType getDefault() {
        return ComponentType.CONTAINER;
    }
}
