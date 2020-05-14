package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;

@RunWith(MockitoJUnitRunner.class)
public class VersionBasedImageFilterTest {

    private static final String CURRENT_CB_VERSION = "2.16";

    private static final String OTHER_CB_VERSION = "2.17";

    private static final String PROPER_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String PROPER_IMAGE_ID_2 = "36cbacf7-f7d4-4875-61f9-548a0acd3512";

    private static final String OTHER_IMAGE_ID = "6edcb9d4-4110-44d8-43f7-d4c0008402a3";

    @Mock
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private PrefixMatcherService prefixMatcherService;

    @InjectMocks
    private VersionBasedImageFilter underTest;

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsImagesWhenThereAreSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(underTest, "cbVersion", CURRENT_CB_VERSION);
        Versions versions = createVersions();
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        List<Image> actual = underTest.getCdhImagesForCbVersion(versions, List.of(properImage, otherImage)).getAvailableImages().getCdhImages();

        assertTrue(actual.contains(properImage));
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsEmptyListWhenThereAreNoSupportedImagesForCbVersion() {
        ReflectionTestUtils.setField(underTest, "cbVersion", "2.18");
        Versions versions = createVersions();
        Image properImage = createImage(PROPER_IMAGE_ID);
        Image otherImage = createImage(OTHER_IMAGE_ID);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.18"), any()))
                .thenReturn(new PrefixMatchImages(Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));

        List<Image> actual = underTest.getCdhImagesForCbVersion(versions, List.of(properImage, otherImage)).getAvailableImages().getCdhImages();

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetCdhImagesForCbVersionShouldReturnsImagesWhenThereAreMultipleSupportedImagesAreAvailableForCbVersion() {
        ReflectionTestUtils.setField(underTest, "cbVersion", CURRENT_CB_VERSION);
        Versions versions = createVersions();
        Image properImage1 = createImage(PROPER_IMAGE_ID);
        Image properImage2 = createImage(PROPER_IMAGE_ID_2);
        Image otherImage = createImage(OTHER_IMAGE_ID);

        List<Image> actual = underTest.getCdhImagesForCbVersion(versions, List.of(properImage1, otherImage, properImage2)).getAvailableImages().getCdhImages();

        assertTrue(actual.contains(properImage1));
        assertTrue(actual.contains(properImage2));
        assertEquals(2, actual.size());
    }

    private Image createImage(String imageId) {
        return new Image(null, null, null, null, imageId, null, null, null, null, null, null, null, null, null);
    }

    private Versions createVersions() {
        return new Versions(List.of(
                new CloudbreakVersion(List.of(CURRENT_CB_VERSION), Collections.emptyList(), List.of(PROPER_IMAGE_ID)),
                new CloudbreakVersion(List.of(CURRENT_CB_VERSION), Collections.emptyList(), List.of(PROPER_IMAGE_ID_2)),
                new CloudbreakVersion(List.of(OTHER_CB_VERSION), Collections.emptyList(), List.of(OTHER_IMAGE_ID))));
    }

}