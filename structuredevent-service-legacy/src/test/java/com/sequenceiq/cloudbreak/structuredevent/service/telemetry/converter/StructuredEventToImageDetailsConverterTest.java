package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredEventToImageDetailsConverterTest {

    private StructuredEventToImageDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToImageDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredFlowEvent) null));
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredSyncEvent) null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPImageDetails flowdetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", flowdetails.getImageCatalog());
        Assert.assertEquals("", flowdetails.getImageId());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPImageDetails syncDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncDetails.getImageCatalog());
        Assert.assertEquals("", syncDetails.getImageId());
    }
}