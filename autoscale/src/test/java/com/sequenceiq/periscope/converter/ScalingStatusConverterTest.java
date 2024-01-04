package com.sequenceiq.periscope.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.ScalingStatus;

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
