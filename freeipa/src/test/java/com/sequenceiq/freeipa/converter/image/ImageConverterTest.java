package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;

class ImageConverterTest {

    private final ImageConverter underTest = new ImageConverter();

    ImageConverterTest() {
        ReflectionTestUtils.setField(underTest, "imageToImageEntityConverter", new ImageToImageEntityConverter());
    }

    @ParameterizedTest
    @CsvSource(value =
            {"false, true",
            "false, false",
            "true, false",
            "true, true"})
    void testConvert(boolean legacyUserData, boolean hasSourceImage) {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setAccountId("accountId");
        imageEntity.setImageId("id");
        imageEntity.setOs("os");
        imageEntity.setOsType("osType");
        imageEntity.setDate("date");
        imageEntity.setLdapAgentVersion("1.0.0");
        imageEntity.setSourceImage(hasSourceImage ? "sourceImage" : null);
        imageEntity.setImageCatalogName("catalogName");
        imageEntity.setImageCatalogUrl("catalogUrl");
        imageEntity.setImdsVersion("v2");
        imageEntity.setSaltVersion("3001.8");
        if (legacyUserData) {
            imageEntity.setUserdata("userData");
        } else {
            imageEntity.setGatewayUserdata("gwUserData");
        }
        Image converted = underTest.convert(imageEntity);

        assertEquals("id", converted.getImageId());
        assertEquals("os", converted.getOs());
        assertEquals("catalogUrl", converted.getImageCatalogUrl());
        assertEquals("catalogName", converted.getImageCatalogName());
        assertEquals("osType", converted.getOsType());
        assertEquals(hasSourceImage ? "sourceImage" : null, converted.getPackageVersion(ImagePackageVersion.SOURCE_IMAGE));
        assertEquals("v2", converted.getPackageVersion(ImagePackageVersion.IMDS_VERSION));
        assertEquals("3001.8", converted.getPackageVersion(ImagePackageVersion.SALT));
        assertEquals(2, converted.getUserdata().keySet().size());
        assertEquals(legacyUserData ? "userData" : "gwUserData", converted.getUserdata().get(InstanceGroupType.GATEWAY));
        assertEquals(legacyUserData ? "userData" : "gwUserData", converted.getUserdata().get(InstanceGroupType.CORE));
    }

    @ParameterizedTest
    @CsvSource(value = {"true", "false"})
    void testConvertFromImageWrapperAndName(boolean hasSourceImage) {
        Map<String, String> packageVersions = Map.of(
                ImagePackageVersion.IMDS_VERSION.getKey(), "v2",
                ImagePackageVersion.SALT.getKey(), "3001.8");
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image image =
                new com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image(100L, "date", "desc", "os", "uuid", Map.of(), "osType",
                        packageVersions, false, "x86_64", Map.of("tagKey", "tagValue"), hasSourceImage ? "sourceImageId" : null);
        ImageWrapper imageWrapper = ImageWrapper.ofFreeipaImage(image, "catalogUrl");
        Pair<ImageWrapper, String> source = Pair.of(imageWrapper, "targetImageName");

        Image converted = underTest.convert(source);

        assertEquals("targetImageName", converted.getImageName());
        assertEquals("os", converted.getOs());
        assertEquals("osType", converted.getOsType());
        assertEquals("uuid", converted.getImageId());
        assertEquals("catalogUrl", converted.getImageCatalogUrl());
        assertEquals(hasSourceImage ? "sourceImageId" : null, converted.getPackageVersion(ImagePackageVersion.SOURCE_IMAGE));
        assertEquals("v2", converted.getPackageVersion(ImagePackageVersion.IMDS_VERSION));
        assertEquals("3001.8", converted.getPackageVersion(ImagePackageVersion.SALT));
        assertEquals("date", converted.getDate());
        assertEquals(100L, converted.getCreated());
        assertEquals(Map.of("tagKey", "tagValue"), converted.getTags());
        assertTrue(converted.getUserdata().isEmpty());
    }
}
