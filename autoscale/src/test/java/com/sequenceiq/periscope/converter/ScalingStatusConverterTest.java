package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.ScalingStatus;

import javax.persistence.AttributeConverter;

public class ScalingStatusConverterTest extends DefaultEnumConverterBaseTest<ScalingStatus> {

    @Override
    public ScalingStatus getDefaultValue() {
        return ScalingStatus.SUCCESS;
    }

    @Override
    public AttributeConverter<ScalingStatus, String> getVictim() {
        return new ScalingStatusConverter();
    }
}