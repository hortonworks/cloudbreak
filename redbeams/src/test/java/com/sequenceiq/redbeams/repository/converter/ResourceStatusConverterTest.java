package com.sequenceiq.redbeams.repository.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;

public class ResourceStatusConverterTest extends DefaultEnumConverterBaseTest<ResourceStatus> {

    @Override
    public ResourceStatus getDefaultValue() {
        return ResourceStatus.UNKNOWN;
    }

    @Override
    public AttributeConverter<ResourceStatus, String> getVictim() {
        return new ResourceStatusConverter();
    }
}
