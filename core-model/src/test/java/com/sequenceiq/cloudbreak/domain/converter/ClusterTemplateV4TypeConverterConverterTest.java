package com.sequenceiq.cloudbreak.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;

class ClusterTemplateV4TypeConverterConverterTest {

    private final ClusterTemplateV4TypeConverter clusterTemplateV4TypeConverter = new ClusterTemplateV4TypeConverter();

    @Test
    void testConvertToDatabaseColumnWhenDatascienceIsReturnAsDatascience() {
        String defaultString = clusterTemplateV4TypeConverter.convertToDatabaseColumn(ClusterTemplateV4Type.DATASCIENCE);
        assertEquals("DATASCIENCE", defaultString);
    }

    @Test
    void testConvertToEntityAttributeWheDatascienceIsReturnAsDatascience() {
        ClusterTemplateV4Type clusterTemplateV4Type = clusterTemplateV4TypeConverter.convertToEntityAttribute("DATASCIENCE");
        assertEquals(ClusterTemplateV4Type.DATASCIENCE, clusterTemplateV4Type);
    }

    @Test
    void testConvertToEntityAttributeWhenNotKnownIsReturnAsOther() {
        ClusterTemplateV4Type clusterTemplateV4Type = clusterTemplateV4TypeConverter.convertToEntityAttribute("not-known");
        assertEquals(ClusterTemplateV4Type.OTHER, clusterTemplateV4Type);
    }

}