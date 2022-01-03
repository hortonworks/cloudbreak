package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;

class ImageProviderTest {

    private static final String CURRENT_IMAGE_ID = "current-image";

    private final ImageProvider underTest = new ImageProvider();

    @Test
    public void testGetCurrentImageFromCatalogShouldReturnTheCurrentImage() throws CloudbreakImageNotFoundException {
        Image actual = underTest.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, createImageCatalog(List.of("other-image1", CURRENT_IMAGE_ID, "other-image2")));
        assertEquals(CURRENT_IMAGE_ID, actual.getUuid());
    }

    @Test
    public void testGetCurrentImageFromCatalogShouldThrowExceptionWhenTheCurrentImageIsNotFound() {
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of("other-image1", "other-image2"));
        Exception exception = assertThrows(CloudbreakImageNotFoundException.class, () -> underTest.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog));
        assertEquals("Image not found with id: current-image", exception.getMessage());
    }

    private CloudbreakImageCatalogV3 createImageCatalog(List<String> images) {
        return new CloudbreakImageCatalogV3(new Images(Collections.emptyList(), images.stream().map(this::createImage).collect(Collectors.toList()),
                Collections.emptyList(), Collections.emptySet()), null);
    }

    private Image createImage(String imageId) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, true, null, null);
    }

}