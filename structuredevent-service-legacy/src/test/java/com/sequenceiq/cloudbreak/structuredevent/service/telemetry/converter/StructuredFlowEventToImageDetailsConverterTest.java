package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

class StructuredFlowEventToImageDetailsConverterTest {

    private StructuredFlowEventToImageDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToImageDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert(null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPImageDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", details.getImageCatalog());
        Assert.assertEquals("", details.getImageId());
    }
}