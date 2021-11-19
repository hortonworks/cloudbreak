package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredEventToImageDetailsConverterTest {

    private static final String IMAGE_ID = "image id";

    private static final String OS_TYPE = "os type";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

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
        Assert.assertEquals("", flowdetails.getOsType());
        Assert.assertEquals("", flowdetails.getImageCatalogUrl());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPImageDetails syncDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncDetails.getImageCatalog());
        Assert.assertEquals("", syncDetails.getImageId());
        Assert.assertEquals("", syncDetails.getOsType());
        Assert.assertEquals("", syncDetails.getImageCatalogUrl());
    }

    @Test
    public void testConversionWithFilledOutValuesStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());

        UsageProto.CDPImageDetails flowdetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(IMAGE_CATALOG, flowdetails.getImageCatalog());
        Assert.assertEquals(IMAGE_ID, flowdetails.getImageId());
        Assert.assertEquals(IMAGE_CATALOG_URL, flowdetails.getImageCatalogUrl());
        Assert.assertEquals(OS_TYPE, flowdetails.getOsType());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());

        UsageProto.CDPImageDetails syncDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals(IMAGE_CATALOG, syncDetails.getImageCatalog());
        Assert.assertEquals(IMAGE_ID, syncDetails.getImageId());
        Assert.assertEquals(IMAGE_CATALOG_URL, syncDetails.getImageCatalogUrl());
        Assert.assertEquals(OS_TYPE, syncDetails.getOsType());
    }

    private StackDetails createStackDetails() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setImage(createImageDetails());
        return stackDetails;
    }

    private ImageDetails createImageDetails() {
        ImageDetails imageDetails = new ImageDetails();
        imageDetails.setImageId(IMAGE_ID);
        imageDetails.setOsType(OS_TYPE);
        imageDetails.setImageCatalogName(IMAGE_CATALOG);
        imageDetails.setImageCatalogUrl(IMAGE_CATALOG_URL);
        return imageDetails;
    }
}