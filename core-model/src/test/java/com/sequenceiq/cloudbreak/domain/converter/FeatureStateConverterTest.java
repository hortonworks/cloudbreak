package com.sequenceiq.cloudbreak.domain.converter;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;

public class FeatureStateConverterTest {

    private final FeatureStateConverter featureStateConverter = new FeatureStateConverter();

    @Test
    public void testConvertToDatabaseColumnWhenReleasedIsReturnAsReleased() {
        String defaultString = featureStateConverter.convertToDatabaseColumn(FeatureState.RELEASED);
        Assert.assertEquals("RELEASED", defaultString);
    }

    @Test
    public void testConvertToEntityAttributeWhenReleasedIsReturnAsReleased() {
        FeatureState featureState = featureStateConverter.convertToEntityAttribute("RELEASED");
        Assert.assertEquals(FeatureState.RELEASED, featureState);
    }

    @Test
    public void testConvertToEntityAttributeWhenNotKnownIsReturnAsReleased() {
        FeatureState featureState = featureStateConverter.convertToEntityAttribute("not-known");
        Assert.assertEquals(FeatureState.RELEASED, featureState);
    }

}