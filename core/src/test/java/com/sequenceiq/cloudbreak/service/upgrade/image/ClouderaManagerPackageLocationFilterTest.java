package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

class ClouderaManagerPackageLocationFilterTest {

    private ClouderaManagerPackageLocationFilter underTest = new ClouderaManagerPackageLocationFilter();

    @Test
    public void testImageNull() {
        boolean result = underTest.filterImage(null, mock(Image.class));

        assertFalse(result);
    }

    @Test
    public void testRepoNull() {
        boolean result = underTest.filterImage(mock(Image.class), mock(Image.class));

        assertFalse(result);
    }

    @Test
    public void testCurrentImageNull() {
        Image image = mock(Image.class);
        when(image.getRepo()).thenReturn(Map.of());
        boolean result = underTest.filterImage(image, null);

        assertFalse(result);
    }

    @Test
    public void testCurrentImageOsTypeEmpty() {
        Image image = mock(Image.class);
        when(image.getRepo()).thenReturn(Map.of());
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn(" ");
        boolean result = underTest.filterImage(image, currentImage);

        assertFalse(result);
    }

    @Test
    public void testOsTypeMissing() {
        Image image = mock(Image.class);
        when(image.getRepo()).thenReturn(Map.of());
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn("redhat7");
        boolean result = underTest.filterImage(image, currentImage);

        assertFalse(result);
    }

    @Test
    public void testNotMatching() {
        Image image = mock(Image.class);
        when(image.getRepo()).thenReturn(Map.of("redhat7", "http://random.org/asdf/"));
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn("redhat7");
        boolean result = underTest.filterImage(image, currentImage);

        assertFalse(result);
    }

    @Test
    public void testMatching() {
        Image image = mock(Image.class);
        when(image.getRepo()).thenReturn(Map.of("redhat7", "http://archive.cloudera.com/asdf/"));
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn("redhat7");
        boolean result = underTest.filterImage(image, currentImage);

        assertTrue(result);
    }

}