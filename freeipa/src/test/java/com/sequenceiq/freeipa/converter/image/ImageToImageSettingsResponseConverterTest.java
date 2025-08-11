package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.entity.ImageEntity;

@ExtendWith(MockitoExtension.class)
class ImageToImageSettingsResponseConverterTest {

    private static final String SOURCE_IMAGE = "cloudera:cdp-7_2_9:runtime-7_2_9:11.33159397.1688602524";

    @Mock
    private ImageToImageEntityConverter imageEntityConverter;

    @InjectMocks
    private ImageToImageSettingsResponseConverter underTest;

    @Test
    void testConversion() {
        ImageEntity source = new ImageEntity();
        source.setImageId("imgid");
        source.setImageCatalogUrl("caturl");
        source.setOs("manjaro");
        source.setLdapAgentVersion("1.2.3");
        source.setSourceImage(SOURCE_IMAGE);

        ImageSettingsResponse result = underTest.convert(source);

        assertEquals(source.getImageId(), result.getId());
        assertEquals(source.getImageCatalogUrl(), result.getCatalog());
        assertEquals(source.getOs(), result.getOs());
        assertEquals(source.getLdapAgentVersion(), result.getLdapAgentVersion());
        assertEquals(source.getSourceImage(), result.getSourceImage());
    }

    @Test
    void testCloudImageConversion() {
        com.sequenceiq.cloudbreak.cloud.model.Image source =
                new Image("imgname", Map.of(), "alma", "rocky", "", "url", "name", "imgid",
                        Map.of("freeipa-ldap-agent", "1.2.3",
                                "source-image", SOURCE_IMAGE), "2019-10-24", 1571884856L, null);
        when(imageEntityConverter.extractLdapAgentVersion(source)).thenReturn("1.2.3");
        when(imageEntityConverter.extractSourceImage(source)).thenReturn(SOURCE_IMAGE);

        ImageSettingsResponse result = underTest.convert(source);

        assertEquals(source.getImageId(), result.getId());
        assertEquals(source.getImageCatalogUrl(), result.getCatalog());
        assertEquals(source.getOs(), result.getOs());
        assertEquals("1.2.3", result.getLdapAgentVersion());
        assertEquals(SOURCE_IMAGE, result.getSourceImage());
    }

    @Test
    void testCloudImageConversionWhenSourceNull() {
        com.sequenceiq.cloudbreak.cloud.model.Image source = null;

        ImageSettingsResponse result = underTest.convert(source);

        assertNull(result);
    }
}