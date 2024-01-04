package com.sequenceiq.datalake.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class SdxClusterShapeConverterTest extends DefaultEnumConverterBaseTest<SdxClusterShape> {

    @Override
    public SdxClusterShape getDefaultValue() {
        return SdxClusterShape.LIGHT_DUTY;
    }

    @Override
    public AttributeConverter<SdxClusterShape, String> getVictim() {
        return new SdxClusterShapeConverter();
    }
}
