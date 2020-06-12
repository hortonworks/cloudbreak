package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

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