package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.Architecture;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;

class DistroXImageToImageSettingsConverterTest {

    private DistroXImageToImageSettingsConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DistroXImageToImageSettingsConverter();
    }

    @Test
    void testConvertDistroXImageV1RequestWithoutArchitecture() {
        DistroXImageV1Request input = new DistroXImageV1Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");

        ImageSettingsV4Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getCatalog(), result.getCatalog());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getOs(), result.getOs());
        assertNull(result.getArchitecture());
    }

    @Test
    void testConvertDistroXImageV1RequestWithValidArchitecture() {
        DistroXImageV1Request input = new DistroXImageV1Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");
        input.setArchitecture("arm64");

        ImageSettingsV4Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getCatalog(), result.getCatalog());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getOs(), result.getOs());
        assertEquals(input.getArchitecture(), result.getArchitecture().getName());
    }

    @Test
    void testConvertDistroXImageV1RequestWithInvalidArchitecture() {
        DistroXImageV1Request input = new DistroXImageV1Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");
        input.setArchitecture("invalid");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> underTest.convert(input));
        assertEquals("Architecture 'invalid' is not supported", exception.getMessage());
    }

    @Test
    void testConvertImageSettingsV4RequestWithoutArchitecture() {
        ImageSettingsV4Request input = new ImageSettingsV4Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");

        DistroXImageV1Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getCatalog(), result.getCatalog());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getOs(), result.getOs());
        assertNull(result.getArchitecture());
    }

    @Test
    void testConvertImageSettingsV4RequestWithArchitecture() {
        ImageSettingsV4Request input = new ImageSettingsV4Request();
        input.setCatalog("someCatalog");
        input.setId("someId");
        input.setOs("someOs");
        input.setArchitecture(Architecture.X86_64);

        DistroXImageV1Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getCatalog(), result.getCatalog());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getOs(), result.getOs());
        assertEquals(input.getArchitecture().getName(), result.getArchitecture());
    }

}