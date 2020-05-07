package com.sequenceiq.cloudbreak.domain.converter;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;

public class DatalakeRequiredConverterTest {

    private final DatalakeRequiredConverter datalakeRequiredConverter = new DatalakeRequiredConverter();

    @Test
    public void testConvertToDatabaseColumnWhenRequiredIsReturnAsRequired() {
        String defaultString = datalakeRequiredConverter.convertToDatabaseColumn(DatalakeRequired.REQUIRED);
        Assert.assertEquals("REQUIRED", defaultString);
    }

    @Test
    public void testConvertToEntityAttributeWheRequiredIsReturnAsRequired() {
        DatalakeRequired datalakeRequired = datalakeRequiredConverter.convertToEntityAttribute("REQUIRED");
        Assert.assertEquals(DatalakeRequired.REQUIRED, datalakeRequired);
    }

    @Test
    public void testConvertToEntityAttributeWhenNotKnownIsReturnAsOptional() {
        DatalakeRequired datalakeRequired = datalakeRequiredConverter.convertToEntityAttribute("not-known");
        Assert.assertEquals(DatalakeRequired.OPTIONAL, datalakeRequired);
    }

}