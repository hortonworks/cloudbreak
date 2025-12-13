package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
public class StackDetailsToCDPImageDetailsConverterTest {

    private static final String IMAGE_ID = "image id";

    private static final String IMAGE_NAME = "image name";

    private static final String OS_TYPE = "os type";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String IMAGE_ARCHITECTURE = Architecture.X86_64.getName().toLowerCase();

    private StackDetailsToCDPImageDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StackDetailsToCDPImageDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPImageDetails cdpImageDetails = underTest.convert(null);

        assertEquals("", cdpImageDetails.getImageCatalog());
        assertEquals("", cdpImageDetails.getImageId());
        assertEquals("", cdpImageDetails.getImageCatalogUrl());
        assertEquals("", cdpImageDetails.getOsType());
        assertEquals("", cdpImageDetails.getImageName());
        assertEquals("", cdpImageDetails.getImageArchitecture());
    }

    @Test
    public void testConvertWithEmpty() {
        UsageProto.CDPImageDetails cdpImageDetails = underTest.convert(new StackDetails());

        assertEquals("", cdpImageDetails.getImageCatalog());
        assertEquals("", cdpImageDetails.getImageId());
        assertEquals("", cdpImageDetails.getImageCatalogUrl());
        assertEquals("", cdpImageDetails.getOsType());
        assertEquals("", cdpImageDetails.getImageName());
        assertEquals("", cdpImageDetails.getImageArchitecture());
    }

    @Test
    public void testConvertWithEmptyImage() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setImage(new ImageDetails());
        UsageProto.CDPImageDetails cdpImageDetails = underTest.convert(stackDetails);

        assertEquals("", cdpImageDetails.getImageCatalog());
        assertEquals("", cdpImageDetails.getImageId());
        assertEquals("", cdpImageDetails.getImageCatalogUrl());
        assertEquals("", cdpImageDetails.getOsType());
        assertEquals("", cdpImageDetails.getImageName());
        assertEquals("x86_64", cdpImageDetails.getImageArchitecture());
    }

    @Test
    public void testConversionWithValues() {
        StackDetails stackDetails = new StackDetails();
        ImageDetails imageDetails = new ImageDetails();
        imageDetails.setImageId(IMAGE_ID);
        imageDetails.setOsType(OS_TYPE);
        imageDetails.setImageCatalogName(IMAGE_CATALOG);
        imageDetails.setImageCatalogUrl(IMAGE_CATALOG_URL);
        imageDetails.setImageName(IMAGE_NAME);
        imageDetails.setImageArchitecture(IMAGE_ARCHITECTURE);
        stackDetails.setImage(imageDetails);

        UsageProto.CDPImageDetails cdpImageDetails = underTest.convert(stackDetails);

        assertEquals(IMAGE_CATALOG, cdpImageDetails.getImageCatalog());
        assertEquals(IMAGE_ID, cdpImageDetails.getImageId());
        assertEquals(IMAGE_CATALOG_URL, cdpImageDetails.getImageCatalogUrl());
        assertEquals(OS_TYPE, cdpImageDetails.getOsType());
        assertEquals(IMAGE_NAME, cdpImageDetails.getImageName());
        assertEquals(IMAGE_ARCHITECTURE, cdpImageDetails.getImageArchitecture());
    }
}
