package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceLifeCycle;

import javax.persistence.AttributeConverter;

public class InstanceLifeCycleConverterTest extends DefaultEnumConverterBaseTest<InstanceLifeCycle> {

    @Override
    public InstanceLifeCycle getDefaultValue() {
        return InstanceLifeCycle.NORMAL;
    }

    @Override
    public AttributeConverter<InstanceLifeCycle, String> getVictim() {
        return new InstanceLifeCycleConverter();
    }
}