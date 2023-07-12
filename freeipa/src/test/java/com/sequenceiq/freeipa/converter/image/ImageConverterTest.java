package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.entity.ImageEntity;

class ImageConverterTest {

    private final ImageConverter underTest = new ImageConverter();

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
        assertEquals(hasSourceImage ? "sourceImage" : null, converted.getPackageVersions().get("source-image"));
        assertEquals(2, converted.getUserdata().keySet().size());
        assertEquals(legacyUserData ? "userData" : "gwUserData", converted.getUserdata().get(InstanceGroupType.GATEWAY));
        assertEquals(legacyUserData ? "userData" : "gwUserData", converted.getUserdata().get(InstanceGroupType.CORE));

    }
}