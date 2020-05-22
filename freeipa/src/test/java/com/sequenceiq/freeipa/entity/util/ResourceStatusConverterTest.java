package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.model.ResourceStatus;

import javax.persistence.AttributeConverter;

public class ResourceStatusConverterTest extends DefaultEnumConverterBaseTest<ResourceStatus> {

    @Override
    public ResourceStatus getDefaultValue() {
        return ResourceStatus.DEFAULT;
    }

    @Override
    public AttributeConverter<ResourceStatus, String> getVictim() {
        return new ResourceStatusConverter();
    }
}