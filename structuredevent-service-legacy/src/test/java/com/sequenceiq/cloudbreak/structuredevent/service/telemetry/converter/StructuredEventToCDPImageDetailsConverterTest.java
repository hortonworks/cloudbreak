package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.common.model.Architecture;

class StructuredEventToCDPImageDetailsConverterTest {

    private static final String IMAGE_ID = "image id";

    private static final String IMAGE_NAME = "image name";

    private static final String OS_TYPE = "os type";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String IMAGE_ARCHITECTURE = Architecture.X86_64.getName().toLowerCase();

    private StructuredEventToCDPImageDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToCDPImageDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        assertNotNull(underTest.convert((StructuredFlowEvent) null), "We should return empty object for not null");
        assertNotNull(underTest.convert((StructuredSyncEvent) null), "We should return empty object for not null");
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPImageDetails flowdetails = underTest.convert(structuredFlowEvent);

        assertEquals("", flowdetails.getImageCatalog());
        assertEquals("", flowdetails.getImageId());
        assertEquals("", flowdetails.getOsType());
        assertEquals("", flowdetails.getImageCatalogUrl());
        assertEquals("", flowdetails.getImageName());
        assertEquals("", flowdetails.getImageArchitecture());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPImageDetails syncDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", syncDetails.getImageCatalog());
        assertEquals("", syncDetails.getImageId());
        assertEquals("", syncDetails.getOsType());
        assertEquals("", syncDetails.getImageCatalogUrl());
        assertEquals("", syncDetails.getImageName());
        assertEquals("", flowdetails.getImageArchitecture());
    }

    @Test
    public void testConversionWithFilledOutValuesStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());

        UsageProto.CDPImageDetails flowdetails = underTest.convert(structuredFlowEvent);

        assertEquals(IMAGE_CATALOG, flowdetails.getImageCatalog());
        assertEquals(IMAGE_ID, flowdetails.getImageId());
        assertEquals(IMAGE_CATALOG_URL, flowdetails.getImageCatalogUrl());
        assertEquals(OS_TYPE, flowdetails.getOsType());
        assertEquals(IMAGE_NAME, flowdetails.getImageName());
        assertEquals(IMAGE_ARCHITECTURE, flowdetails.getImageArchitecture());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());

        UsageProto.CDPImageDetails syncDetails = underTest.convert(structuredSyncEvent);

        assertEquals(IMAGE_CATALOG, syncDetails.getImageCatalog());
        assertEquals(IMAGE_ID, syncDetails.getImageId());
        assertEquals(IMAGE_CATALOG_URL, syncDetails.getImageCatalogUrl());
        assertEquals(OS_TYPE, syncDetails.getOsType());
        assertEquals(IMAGE_NAME, syncDetails.getImageName());
        assertEquals(IMAGE_ARCHITECTURE, syncDetails.getImageArchitecture());
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
        imageDetails.setImageName(IMAGE_NAME);
        imageDetails.setImageArchitecture(IMAGE_ARCHITECTURE);
        return imageDetails;
    }
}