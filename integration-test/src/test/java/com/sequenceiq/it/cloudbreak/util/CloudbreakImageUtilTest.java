package com.sequenceiq.it.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class CloudbreakImageUtilTest {

    private static final String CATALOG_URL = "https://catalog/v3-dev-cb-image-catalog.json";

    private static final String OPENSTACK = "OPENSTACK";

    private static final String REGION = "iopscloud";

    private static final String REDHAT9 = "redhat9";

    private ImageCatalogDownloader imageCatalogDownloader;

    private CloudbreakImageUtil underTest;

    @BeforeMethod
    public void setUp() {
        imageCatalogDownloader = mock(ImageCatalogDownloader.class);
        underTest = new CloudbreakImageUtil();
        ReflectionTestUtils.setField(underTest, "imageCatalogDownloader", imageCatalogDownloader);
    }

    @Test
    public void returnsMostRecentlyCreatedBaseImageMatchingProviderRegionOsAndArchitecture() {
        givenBaseImages(List.of(
                baseImage("older", "openstack", REGION, REDHAT9, "x86_64", 100L),
                baseImage("newest-matching", "openstack", REGION, REDHAT9, "x86_64", 200L),
                baseImage("without-created", "openstack", REGION, REDHAT9, "x86_64", null),
                baseImage("other-region", "openstack", "other-region", REDHAT9, "x86_64", 300L),
                baseImage("other-os", "openstack", REGION, "redhat8", "x86_64", 300L),
                baseImage("other-architecture", "openstack", REGION, REDHAT9, "arm64", 300L),
                baseImage("other-provider", "aws", REGION, REDHAT9, "x86_64", 300L)));

        String imageId = underTest.getNewestBaseImageId(CATALOG_URL, OPENSTACK, REGION, REDHAT9, Architecture.X86_64);

        assertThat(imageId).isEqualTo("newest-matching");
    }

    @Test
    public void failsWhenNoBaseImageMatches() {
        givenBaseImages(List.of(baseImage("other-provider", "aws", REGION, REDHAT9, "x86_64", 100L)));

        assertThatThrownBy(() -> underTest.getNewestBaseImageId(CATALOG_URL, OPENSTACK, REGION, REDHAT9, Architecture.X86_64))
                .isInstanceOf(TestFailException.class)
                .hasMessageContaining("Not found any 'redhat9' x86_64 base image on OPENSTACK (region: iopscloud)")
                .hasMessageContaining(CATALOG_URL);
    }

    private void givenBaseImages(List<Map<String, Object>> baseImages) {
        Map<String, Object> catalog = Map.of("images", Map.of("base-images", baseImages));
        when(imageCatalogDownloader.download(CATALOG_URL, CloudbreakImageCatalogV3.class))
                .thenReturn(new ObjectMapper().convertValue(catalog, CloudbreakImageCatalogV3.class));
    }

    private Map<String, Object> baseImage(String uuid, String provider, String region, String osType, String architecture, Long created) {
        Map<String, Object> image = new HashMap<>(Map.of(
                "uuid", uuid,
                "date", "2026-09-22",
                "description", "base image",
                "os", osType,
                "os_type", osType,
                "architecture", architecture,
                "images", Map.of(provider, Map.of(region, "base-" + uuid))));
        if (created != null) {
            image.put("created", created);
        }
        return image;
    }
}
