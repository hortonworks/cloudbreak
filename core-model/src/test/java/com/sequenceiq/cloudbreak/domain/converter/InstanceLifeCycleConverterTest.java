package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

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
