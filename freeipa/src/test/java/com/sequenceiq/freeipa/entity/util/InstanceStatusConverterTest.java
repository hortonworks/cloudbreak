package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;

import javax.persistence.AttributeConverter;

public class InstanceStatusConverterTest extends DefaultEnumConverterBaseTest<InstanceStatus> {

    @Override
    public InstanceStatus getDefaultValue() {
        return InstanceStatus.CREATED;
    }

    @Override
    public AttributeConverter<InstanceStatus, String> getVictim() {
        return new InstanceStatusConverter();
    }
}