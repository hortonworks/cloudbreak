package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;

import javax.persistence.AttributeConverter;

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