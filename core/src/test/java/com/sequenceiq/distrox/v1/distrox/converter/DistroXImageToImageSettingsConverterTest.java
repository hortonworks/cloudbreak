package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;

class DistroXImageToImageSettingsConverterTest {

    private DistroXImageToImageSettingsConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DistroXImageToImageSettingsConverter();
    }

    @Test
    void testConvertDistroXImageV1RequestToImageSettingsV4Request() {
        DistroXImageV1Request input = new DistroXImageV1Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");

        ImageSettingsV4Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getCatalog(), result.getCatalog());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getOs(), result.getOs());
    }

    @Test
    void testConvertImageSettingsV4RequestToDistroXImageV1Request() {
        ImageSettingsV4Request input = new ImageSettingsV4Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");

        DistroXImageV1Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getCatalog(), result.getCatalog());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getOs(), result.getOs());
    }

}