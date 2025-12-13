package com.sequenceiq.cloudbreak.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;

class FeatureStateConverterTest {

    private final FeatureStateConverter featureStateConverter = new FeatureStateConverter();

    @Test
    void testConvertToDatabaseColumnWhenReleasedIsReturnAsReleased() {
        String defaultString = featureStateConverter.convertToDatabaseColumn(FeatureState.RELEASED);
        assertEquals("RELEASED", defaultString);
    }

    @Test
    void testConvertToEntityAttributeWhenReleasedIsReturnAsReleased() {
        FeatureState featureState = featureStateConverter.convertToEntityAttribute("RELEASED");
        assertEquals(FeatureState.RELEASED, featureState);
    }

    @Test
    void testConvertToEntityAttributeWhenNotKnownIsReturnAsReleased() {
        FeatureState featureState = featureStateConverter.convertToEntityAttribute("not-known");
        assertEquals(FeatureState.RELEASED, featureState);
    }

}