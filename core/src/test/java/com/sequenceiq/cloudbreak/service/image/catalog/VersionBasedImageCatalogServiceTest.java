package com.sequenceiq.cloudbreak.service.image.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.CloudbreakVersionListProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogVersionFilter;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.PrefixMatchImages;
import com.sequenceiq.cloudbreak.service.image.PrefixMatcherService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;

@ExtendWith(MockitoExtension.class)
public class VersionBasedImageCatalogServiceTest {

    private static final String CURRENT_CB_VERSION = "2.16";

    private static final String OTHER_CB_VERSION = "2.17";

    private static final String PROPER_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String PROPER_IMAGE_ID_2 = "36cbacf7-f7d4-4875-61f9-548a0acd3512";

    private static final String OTHER_IMAGE_ID = "6edcb9d4-4110-44d8-43f7-d4c0008402a3";

    @Mock
    private VersionBasedImageProvider versionBasedImageProvider;

    @Mock
    private RawImageProvider rawImageProvider;

    @Mock
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private PrefixMatcherService prefixMatcherService;

    @Mock
    private CloudbreakImageCatalogV3 imageCatalogV3;

    @Mock
    private ImageFilter imageFilter;

    @Mock
    private Images images;

    @Mock
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    @InjectMocks
    private VersionBasedImageCatalogService victim;

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsImagesWhenThereAreSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", CURRENT_CB_VERSION);
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);
        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getCdhImages()).thenReturn(List.of(properImage, otherImage));

        Versions versions = createCbVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getCloudbreakVersions());

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.contains(properImage));
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetFreeipaImagesForCbVersionShouldReturnsImagesWhenThereAreSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", CURRENT_CB_VERSION);
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);
        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getFreeIpaImages()).thenReturn(List.of(properImage, otherImage));

        Versions versions = createFreeipaVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getFreeipaVersions());

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.contains(properImage));
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsEmptyListWhenThereAreNoSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", "2.18");
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(imageCatalogV3.getImages()).thenReturn(images);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.18"), any()))
                .thenReturn(new PrefixMatchImages(Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        when(images.getCdhImages()).thenReturn(List.of(properImage, otherImage));

        Versions versions = createCbVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getCloudbreakVersions());

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetFreeipaImagesForCbVersionShouldReturnsEmptyListWhenThereAreNoSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", "2.18");
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(imageCatalogV3.getImages()).thenReturn(images);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.18"), any()))
                .thenReturn(new PrefixMatchImages(Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        when(images.getFreeIpaImages()).thenReturn(List.of(properImage, otherImage));

        Versions versions = createFreeipaVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getFreeipaVersions());

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsImagesWhenThereAreMultipleSupportedImagesAreAvailableForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", CURRENT_CB_VERSION);
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getCdhImages()).thenReturn(List.of(properImage1, otherImage, properImage2));

        Versions versions = createCbVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getCloudbreakVersions());

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.contains(properImage1));
        assertTrue(actual.contains(properImage2));
        assertEquals(2, actual.size());
    }

    @Test
    public void testGetFreeipaImagesForCbVersionShouldReturnsImagesWhenThereAreMultipleSupportedImagesAreAvailableForCbVersion() {
        ReflectionTestUtils.setField(victim, "cbVersion", CURRENT_CB_VERSION);
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getFreeIpaImages()).thenReturn(List.of(properImage1, otherImage, properImage2));

        Versions versions = createFreeipaVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getFreeipaVersions());

        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.contains(properImage1));
        assertTrue(actual.contains(properImage2));
        assertEquals(2, actual.size());
    }

    @Test
    public void testValidateWithNullVersionBlock() {
        Exception exception = assertThrows(CloudbreakImageCatalogException.class, () -> victim.validate(createCatalog(null)));

        assertEquals("Cloudbreak versions cannot be NULL", exception.getMessage());
    }

    @Test
    public void testValidateWithEmptyVersionBlock() {
        CloudbreakImageCatalogV3 catalog = createCatalog(new Versions(Collections.emptyList(), Collections.emptyList()));
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(catalog.getVersions().getCloudbreakVersions());

        Exception exception = assertThrows(CloudbreakImageCatalogException.class, () -> victim.validate(catalog));

        assertEquals("Cloudbreak versions cannot be NULL", exception.getMessage());
    }

    @Test
    public void testValidateCbImagesWhichAllStoredInVersionBlock() throws CloudbreakImageCatalogException {
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(images.getCdhImages()).thenReturn(List.of(properImage1, properImage2, otherImage));

        Versions versions = createCbVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getCloudbreakVersions());

        victim.validate(createCatalog(versions));
    }

    @Test
    public void testValidateCbImagesWhichAnyStoredInVersionBlock() {
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);

        when(images.getCdhImages()).thenReturn(List.of(properImage1, properImage2));

        Versions versions = createCbVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getCloudbreakVersions());

        Exception exception = assertThrows(CloudbreakImageCatalogException.class, () -> victim.validate(createCatalog(versions)));

        assertEquals("Images with ids: " + OTHER_IMAGE_ID + " is not present in cdh-images block", exception.getMessage());
    }

    @Test
    public void testValidateFreeipaImagesWhichAllStoredInVersionBlock() throws CloudbreakImageCatalogException {
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        when(images.getFreeIpaImages()).thenReturn(List.of(properImage1, properImage2, otherImage));

        Versions versions = createFreeipaVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getFreeipaVersions());

        victim.validate(createCatalog(versions));
    }

    @Test
    public void testValidateFreeipaImagesWhichAnyStoredInVersionBlock() {
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);

        when(images.getCdhImages()).thenReturn(List.of(properImage1, properImage2));

        Versions versions = createFreeipaVersions();
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(versions.getFreeipaVersions());

        Exception exception = assertThrows(CloudbreakImageCatalogException.class, () -> victim.validate(createCatalog(versions)));

        assertEquals("Images with ids: " + OTHER_IMAGE_ID + " is not present in cdh-images block", exception.getMessage());
    }

    @Test
    public void testGetImagesShouldCallVersionBasedImageProviderWhenTheCbVersionIsPresent() {
        StatedImages statedImages = mock(StatedImages.class);
        when(imageFilter.getCbVersion()).thenReturn("2.49");
        when(versionBasedImageProvider.getImages(imageCatalogV3, imageFilter)).thenReturn(statedImages);

        StatedImages actual = victim.getImages(imageCatalogV3, imageFilter);

        assertEquals(statedImages, actual);
        verify(versionBasedImageProvider).getImages(imageCatalogV3, imageFilter);
    }

    @Test
    public void testGetImagesShouldCallRawImageProviderWhenTheCbVersionIsNotPresent() {
        StatedImages statedImages = mock(StatedImages.class);
        when(rawImageProvider.getImages(imageCatalogV3, imageFilter)).thenReturn(statedImages);

        StatedImages actual = victim.getImages(imageCatalogV3, imageFilter);

        assertEquals(statedImages, actual);
        verify(rawImageProvider).getImages(imageCatalogV3, imageFilter);
    }

    private Image createImage(String imageId) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, true, null, null);
    }

    private Versions createCbVersions() {
        return new Versions(createCloudbreakVersionList(), null);
    }

    private Versions createFreeipaVersions() {
        return new Versions(null, createCloudbreakVersionList());
    }

    private List<CloudbreakVersion> createCloudbreakVersionList() {
        return List.of(
                new CloudbreakVersion(List.of(CURRENT_CB_VERSION), Collections.emptyList(), List.of(PROPER_IMAGE_ID)),
                new CloudbreakVersion(List.of(CURRENT_CB_VERSION), Collections.emptyList(), List.of(PROPER_IMAGE_ID_2)),
                new CloudbreakVersion(List.of(OTHER_CB_VERSION), Collections.emptyList(), List.of(OTHER_IMAGE_ID)));
    }

    private CloudbreakImageCatalogV3 createCatalog(Versions versions) {
        return new CloudbreakImageCatalogV3(images, versions);
    }
}