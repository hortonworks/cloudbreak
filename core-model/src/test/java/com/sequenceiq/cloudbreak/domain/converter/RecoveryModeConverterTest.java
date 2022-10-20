package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

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