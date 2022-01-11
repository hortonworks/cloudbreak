package com.sequenceiq.cloudbreak.service.image.catalog;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.LatestDefaultImageUuidProvider;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdvertisedImageProviderTest {

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String PLATFORM_AWS = "AWS";

    private static final String PLATFORM_AZURE = "Azure";

    private static final String BASE_IMAGE_ID_AWS_ADVERTISED = "Base image AWS advertised";

    private static final String BASE_IMAGE_ID_AWS_NON_ADVERTISED = "Base image AWS non advertised";

    private static final String BASE_IMAGE_ID_AZURE_ADVERTISED = "Base image Azure advertised";

    private static final String BASE_IMAGE_ID_AZURE_NON_ADVERTISED = "Base image Azure non advertised";

    private static final String BASE_IMAGE_ID_AWS_AZURE_ADVERTISED = "Base image AWS & Azure advertised";

    private static final String BASE_IMAGE_ID_AWS_AZURE_NON_ADVERTISED = "Base image AWS & Azure non advertised";

    private static final String CDH_IMAGE_ID_AWS_ADVERTISED = "CDH image AWS advertised";

    private static final String CDH_IMAGE_ID_AWS_NON_ADVERTISED = "CDH image AWS non advertised";

    private static final String CDH_IMAGE_ID_AZURE_ADVERTISED = "CDH image Azure advertised";

    private static final String CDH_IMAGE_ID_AZURE_NON_ADVERTISED = "CDH image Azure non advertised";

    private static final String CDH_IMAGE_ID_AWS_AZURE_ADVERTISED = "CDH image AWS & Azure advertised";

    private static final String CDH_IMAGE_ID_AWS_AZURE_NON_ADVERTISED = "CDH image AWS & Azure non advertised";

    @Mock
    private LatestDefaultImageUuidProvider latestDefaultImageUuidProvider;

    @InjectMocks
    private AdvertisedImageProvider victim;

    @BeforeEach
    public void initTests() {
        when(latestDefaultImageUuidProvider.getLatestDefaultImageUuids(any(), any())).thenReturn(emptySet());
    }

    @Test
    public void shouldNotIncludeBaseImagesWhenBaseImagesAreDisabled() {
        CloudbreakImageCatalogV3 imageCatalog = anImageCatalogV3();
        StatedImages actual = victim.getImages(anImageCatalogV3(), createImageFilter(false));

        assertFalse(imageCatalog.getImages().getBaseImages().isEmpty());
        assertTrue(actual.getImages().getBaseImages().isEmpty());
    }

    @Test
    public void shouldIncludeBaseImagesWhenBaseImagesAreEnabled() {
        CloudbreakImageCatalogV3 imageCatalog = anImageCatalogV3();
        StatedImages actual = victim.getImages(anImageCatalogV3(), createImageFilter(true));

        assertFalse(imageCatalog.getImages().getBaseImages().isEmpty());
        assertFalse(actual.getImages().getBaseImages().isEmpty());
    }

    @Test
    public void shouldSetUpImageCatalogNameAndUrl() {
        ImageFilter imageFilter = createImageFilter(false);
        StatedImages actual = victim.getImages(anImageCatalogV3(), createImageFilter(false));

        assertEquals(imageFilter.getImageCatalog().getImageCatalogUrl(), actual.getImageCatalogUrl());
        assertEquals(imageFilter.getImageCatalog().getName(), actual.getImageCatalogName());
    }

    @Test
    public void shouldHaveImagesWithAdvertisedFlagAndWithTheEnabledPlatforms() {
        StatedImages actual = victim.getImages(anImageCatalogV3(), createImageFilter(true));

        assertTrue(imageAvailableWithId(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AWS_ADVERTISED));
        assertTrue(imageAvailableWithId(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AWS_AZURE_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AWS_NON_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AWS_AZURE_NON_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AZURE_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AZURE_NON_ADVERTISED));

        assertTrue(imageAvailableWithId(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AWS_ADVERTISED));
        assertTrue(imageAvailableWithId(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AWS_AZURE_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AWS_NON_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AWS_AZURE_NON_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AZURE_ADVERTISED));
        assertFalse(imageAvailableWithId(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AZURE_NON_ADVERTISED));
    }

    @Test
    public void shouldSetDefaultFlags() {
        when(latestDefaultImageUuidProvider.getLatestDefaultImageUuids(any(), any()))
                .thenReturn(asList(BASE_IMAGE_ID_AWS_ADVERTISED, CDH_IMAGE_ID_AWS_ADVERTISED).stream().collect(toSet()));

        StatedImages actual = victim.getImages(anImageCatalogV3(), createImageFilter(true));

        assertTrue(imageAvailableWithIdAndDefaultFlag(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AWS_ADVERTISED, true));
        assertTrue(imageAvailableWithIdAndDefaultFlag(actual.getImages().getBaseImages(), BASE_IMAGE_ID_AWS_AZURE_ADVERTISED, false));

        assertTrue(imageAvailableWithIdAndDefaultFlag(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AWS_ADVERTISED, true));
        assertTrue(imageAvailableWithIdAndDefaultFlag(actual.getImages().getCdhImages(), CDH_IMAGE_ID_AWS_AZURE_ADVERTISED, false));
    }

    private CloudbreakImageCatalogV3 anImageCatalogV3() {
        Images images = new Images(baseImages(), cdhImages(), null, null);
        return new CloudbreakImageCatalogV3(images, null);
    }

    private List<Image> baseImages() {
        return asList(
                createImage(BASE_IMAGE_ID_AWS_ADVERTISED, true, PLATFORM_AWS),
                createImage(BASE_IMAGE_ID_AWS_NON_ADVERTISED, false, PLATFORM_AWS),
                createImage(BASE_IMAGE_ID_AZURE_ADVERTISED, true, PLATFORM_AZURE),
                createImage(BASE_IMAGE_ID_AZURE_NON_ADVERTISED, false, PLATFORM_AZURE),
                createImage(BASE_IMAGE_ID_AWS_AZURE_ADVERTISED, true, PLATFORM_AWS, PLATFORM_AZURE),
                createImage(BASE_IMAGE_ID_AWS_AZURE_NON_ADVERTISED, false, PLATFORM_AWS, PLATFORM_AZURE)
        );
    }

    private List<Image> cdhImages() {
        return asList(
                createImage(CDH_IMAGE_ID_AWS_ADVERTISED, true, PLATFORM_AWS),
                createImage(CDH_IMAGE_ID_AWS_NON_ADVERTISED, false, PLATFORM_AWS),
                createImage(CDH_IMAGE_ID_AZURE_ADVERTISED, true, PLATFORM_AZURE),
                createImage(CDH_IMAGE_ID_AZURE_NON_ADVERTISED, false, PLATFORM_AZURE),
                createImage(CDH_IMAGE_ID_AWS_AZURE_ADVERTISED, true, PLATFORM_AWS, PLATFORM_AZURE),
                createImage(CDH_IMAGE_ID_AWS_AZURE_NON_ADVERTISED, false, PLATFORM_AWS, PLATFORM_AZURE)
        );
    }

    private Image createImage(String uuid, boolean advertised, String... platforms) {
        return new Image(null, null, null, null, null, uuid, null, null,
                Arrays.stream(platforms).collect(Collectors.toMap(p -> p, p -> Collections.emptyMap())), null, null, null, null, null, null, advertised,
                null, null);
    }

    private ImageFilter createImageFilter(boolean enableBaseImages) {
        return new ImageFilter(anImageCatalog(), Collections.singleton(PLATFORM_AWS), "CB version", enableBaseImages, null, null);
    }

    private ImageCatalog anImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(IMAGE_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);

        return imageCatalog;
    }

    private boolean imageAvailableWithId(List<Image> images, String uuid) {
        return images.stream().anyMatch(i -> i.getUuid().equals(uuid));
    }

    private boolean imageAvailableWithIdAndDefaultFlag(List<Image> images, String uuid, boolean defaultImage) {
        return images.stream().anyMatch(i -> i.getUuid().equals(uuid) && i.isDefaultImage() == defaultImage);
    }
}
