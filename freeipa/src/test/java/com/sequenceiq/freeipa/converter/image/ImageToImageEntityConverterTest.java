package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.ImageEntity;

class ImageToImageEntityConverterTest {

    private static final String SOURCE_IMAGE = "cloudera:offer:sku:vesion";

    private final ImageToImageEntityConverter underTest = new ImageToImageEntityConverter();

    @Test
    void testConversion() {
        Image source = new Image(1L, "date", "", "arch", "1-2-a-b", Map.of(), "manjaro",
                Map.of("freeipa-ldap-agent", "1.2.3",
                        "source-image", SOURCE_IMAGE,
                        "imds", "v2",
                        ImagePackageVersion.SALT.getKey(), "3001.8"),
                false, "x86_64");

        ImageEntity result = underTest.convert("accountId", source);

        assertEquals(source.getUuid(), result.getImageId());
        assertEquals(source.getOs(), result.getOs());
        assertEquals(source.getOsType(), result.getOsType());
        assertEquals(source.getDate(), result.getDate());
        assertEquals("1.2.3", result.getLdapAgentVersion());
        assertEquals(SOURCE_IMAGE, result.getSourceImage());
        assertEquals("v2", result.getImdsVersion());
        assertEquals("3001.8", result.getSaltVersion());
    }

    @Test
    void testExtracImdsVersion() {
        Image source = new Image(1L, "date", "", "arch", "1-2-a-b", Map.of(), "manjaro", Map.of("imds", "anything"), false, "x86_64");

        String result = underTest.extractImdsVersion(source);

        assertEquals("anything", result);
    }

    @Test
    void testExtractLdapAgentVersion() {
        Image source = new Image(1L, "date", "", "arch", "1-2-a-b", Map.of(), "manjaro", Map.of("freeipa-ldap-agent", "1.2.3"), false, "x86_64");

        String result = underTest.extractLdapAgentVersion(source);

        assertEquals("1.2.3", result);
    }

    @Test
    void testExtractLdapAgentVersionCloudImage() {
        com.sequenceiq.cloudbreak.cloud.model.Image source =
                new com.sequenceiq.cloudbreak.cloud.model.Image("asd", Map.of(), "osss", "type", "", "url", "name", "imid",
                        Map.of("freeipa-ldap-agent", "1.2.3"), "2019-10-24", 1571884856L, null);

        String result = underTest.extractLdapAgentVersion(source);

        assertEquals("1.2.3", result);
    }

    @Test
    void testExtractSourceImageFromCloudImage() {
        com.sequenceiq.cloudbreak.cloud.model.Image source =
                new com.sequenceiq.cloudbreak.cloud.model.Image("asd", Map.of(), "osss", "type", "", "url", "name", "imid",
                        Map.of("source-image", SOURCE_IMAGE), "2019-10-24", 1571884856L, null);

        String result = underTest.extractSourceImage(source);

        assertEquals(SOURCE_IMAGE, result);
    }

    @Test
    void testExtractSourceImage() {
        Image source = new Image(1L, "date", "", "arch", "1-2-a-b", Map.of(), "manjaro", Map.of("source-image", SOURCE_IMAGE), false, "x86_64");

        String result = underTest.extractSourceImage(source);

        assertEquals(SOURCE_IMAGE, result);
    }
}