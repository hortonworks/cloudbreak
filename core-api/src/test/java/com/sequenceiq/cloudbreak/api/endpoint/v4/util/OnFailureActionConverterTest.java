package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class OnFailureActionConverterTest extends DefaultEnumConverterBaseTest<OnFailureAction> {

    @Override
    public OnFailureAction getDefaultValue() {
        return OnFailureAction.DO_NOTHING;
    }

    @Override
    public AttributeConverter<OnFailureAction, String> getVictim() {
        return new OnFailureActionConverter();
    }
}
