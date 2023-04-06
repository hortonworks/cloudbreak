package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.ImageEntity;

class ImageToImageEntityConverterTest {

    private ImageToImageEntityConverter underTest = new ImageToImageEntityConverter();

    @Test
    void testConversion() {
        Image source = new Image(1L, "date", "", "arch", "1-2-a-b", Map.of(), "manjaro", Map.of("freeipa-ldap-agent", "1.2.3"), false);

        ImageEntity result = underTest.convert(source);

        assertEquals(source.getUuid(), result.getImageId());
        assertEquals(source.getOs(), result.getOs());
        assertEquals(source.getOsType(), result.getOsType());
        assertEquals(source.getDate(), result.getDate());
        assertEquals("1.2.3", result.getLdapAgentVersion());
    }

    @Test
    void testExtractLdapAgentVersion() {
        Image source = new Image(1L, "date", "", "arch", "1-2-a-b", Map.of(), "manjaro", Map.of("freeipa-ldap-agent", "1.2.3"), false);

        String result = underTest.extractLdapAgentVersion(source);

        assertEquals("1.2.3", result);
    }
}