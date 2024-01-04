package com.sequenceiq.cloudbreak.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.common.api.type.CommonStatus;

public class CommonStatusConverterTest extends DefaultEnumConverterBaseTest<CommonStatus> {

    @Override
    public CommonStatus getDefaultValue() {
        return CommonStatus.CREATED;
    }

    @Override
    public AttributeConverter<CommonStatus, String> getVictim() {
        return new CommonStatusConverter();
    }
}
