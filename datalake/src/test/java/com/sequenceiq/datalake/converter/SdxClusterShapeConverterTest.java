package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

import javax.persistence.AttributeConverter;

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