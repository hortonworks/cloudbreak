package com.sequenceiq.cloudbreak.service.image.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@ExtendWith(MockitoExtension.class)
public class AdvertisedImageCatalogServiceTest {

    private static final String ADVERTISED_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String NON_ADVERTISED_IMAGE_ID = "36cbacf7-f7d4-4875-61f9-548a0acd3512";

    @InjectMocks
    private AdvertisedImageCatalogService victim;

    @Mock
    private CloudbreakImageCatalogV3 imageCatalogV3;

    @Mock
    private Images images;

    @Mock
    private Versions version;

    @Test
    public void shouldReturnImagesWithAdvertisedFlag() {
        List<Image> cdhImages = List.of(createImage(ADVERTISED_IMAGE_ID, true), createImage(NON_ADVERTISED_IMAGE_ID, false));
        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getCdhImages()).thenReturn(cdhImages);
        List<Image> actual = victim.getImageFilterResult(imageCatalogV3).getImages();

        assertTrue(actual.stream().anyMatch(i -> i.getUuid().equals(ADVERTISED_IMAGE_ID)));
        assertFalse(actual.stream().anyMatch(i -> i.getUuid().equals(NON_ADVERTISED_IMAGE_ID)));
    }

    @Test
    public void validationShouldNotFailInCaseOfNullVersionsAndAdvertisedCdhImages() throws CloudbreakImageCatalogException {
        List<Image> cdhImages = List.of(createImage(ADVERTISED_IMAGE_ID, true), createImage(NON_ADVERTISED_IMAGE_ID, false));
        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getCdhImages()).thenReturn(cdhImages);

        victim.validate(imageCatalogV3);
    }

    @Test
    public void validationShouldFailInCaseOfNoAdvertisedCdhImages() {
        List<Image> cdhImages = List.of(createImage(NON_ADVERTISED_IMAGE_ID, false));
        when(imageCatalogV3.getImages()).thenReturn(images);
        when(images.getCdhImages()).thenReturn(cdhImages);

        assertThrows(CloudbreakImageCatalogException.class, () -> victim.validate(imageCatalogV3));
    }

    @Test
    public void validationShouldFailInCaseOfNonNullVersions() {
        when(imageCatalogV3.getVersions()).thenReturn(version);

        assertThrows(CloudbreakImageCatalogException.class, () -> victim.validate(imageCatalogV3));
    }

    private Image createImage(String imageId, boolean advertised) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, advertised, null, null);
    }
}