package com.sequenceiq.cloudbreak.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;

class DatalakeRequiredConverterTest {

    private final DatalakeRequiredConverter datalakeRequiredConverter = new DatalakeRequiredConverter();

    @Test
    void testConvertToDatabaseColumnWhenRequiredIsReturnAsRequired() {
        String defaultString = datalakeRequiredConverter.convertToDatabaseColumn(DatalakeRequired.REQUIRED);
        assertEquals("REQUIRED", defaultString);
    }

    @Test
    void testConvertToEntityAttributeWheRequiredIsReturnAsRequired() {
        DatalakeRequired datalakeRequired = datalakeRequiredConverter.convertToEntityAttribute("REQUIRED");
        assertEquals(DatalakeRequired.REQUIRED, datalakeRequired);
    }

    @Test
    void testConvertToEntityAttributeWhenNotKnownIsReturnAsOptional() {
        DatalakeRequired datalakeRequired = datalakeRequiredConverter.convertToEntityAttribute("not-known");
        assertEquals(DatalakeRequired.OPTIONAL, datalakeRequired);
    }

}