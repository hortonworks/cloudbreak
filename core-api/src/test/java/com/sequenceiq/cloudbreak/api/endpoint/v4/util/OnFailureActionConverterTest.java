package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

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