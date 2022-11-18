package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.entity.ImageEntity;

class ImageToImageSettingsResponseConverterTest {

    private ImageToImageSettingsResponseConverter underTest = new ImageToImageSettingsResponseConverter();

    @Test
    void testConversion() {
        ImageEntity source = new ImageEntity();
        source.setImageId("imgid");
        source.setImageCatalogUrl("caturl");
        source.setOs("manjaro");
        source.setLdapAgentVersion("1.2.3");

        ImageSettingsResponse result = underTest.convert(source);

        assertEquals(source.getImageId(), result.getId());
        assertEquals(source.getImageCatalogUrl(), result.getCatalog());
        assertEquals(source.getOs(), result.getOs());
        assertEquals(source.getLdapAgentVersion(), result.getLdapAgentVersion());
    }
}