package com.sequenceiq.datalake.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.AttributeConverter;

import org.junit.jupiter.api.Test;

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

    @Test
    public void shouldConvertLegacyWithoutHbaseNamesToProShapes() {
        assertEquals(SdxClusterShape.LIGHT_DUTY_PRO, getVictim().convertToEntityAttribute("LIGHT_DUTY_WITHOUT_HBASE"));
        assertEquals(SdxClusterShape.ENTERPRISE_PRO, getVictim().convertToEntityAttribute("ENTERPRISE_WITHOUT_HBASE"));
    }
}
