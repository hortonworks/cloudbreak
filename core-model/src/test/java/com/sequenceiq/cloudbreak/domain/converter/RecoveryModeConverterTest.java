package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

public class RecoveryModeConverterTest extends DefaultEnumConverterBaseTest<RecoveryMode> {

    @Override
    public RecoveryMode getDefaultValue() {
        return RecoveryMode.AUTO;
    }

    @Override
    public AttributeConverter<RecoveryMode, String> getVictim() {
        return new RecoveryModeConverter();
    }
}