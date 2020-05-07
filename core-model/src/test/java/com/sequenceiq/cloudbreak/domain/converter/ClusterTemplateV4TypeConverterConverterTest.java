package com.sequenceiq.cloudbreak.domain.converter;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;

public class ClusterTemplateV4TypeConverterConverterTest {

    private final ClusterTemplateV4TypeConverter clusterTemplateV4TypeConverter = new ClusterTemplateV4TypeConverter();

    @Test
    public void testConvertToDatabaseColumnWhenDatascienceIsReturnAsDatascience() {
        String defaultString = clusterTemplateV4TypeConverter.convertToDatabaseColumn(ClusterTemplateV4Type.DATASCIENCE);
        Assert.assertEquals("DATASCIENCE", defaultString);
    }

    @Test
    public void testConvertToEntityAttributeWheDatascienceIsReturnAsDatascience() {
        ClusterTemplateV4Type clusterTemplateV4Type = clusterTemplateV4TypeConverter.convertToEntityAttribute("DATASCIENCE");
        Assert.assertEquals(ClusterTemplateV4Type.DATASCIENCE, clusterTemplateV4Type);
    }

    @Test
    public void testConvertToEntityAttributeWhenNotKnownIsReturnAsOther() {
        ClusterTemplateV4Type clusterTemplateV4Type = clusterTemplateV4TypeConverter.convertToEntityAttribute("not-known");
        Assert.assertEquals(ClusterTemplateV4Type.OTHER, clusterTemplateV4Type);
    }

}