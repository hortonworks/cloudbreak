package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;

class CdhPackageLocationFilterTest {

    private final CdhPackageLocationFilter underTest = new CdhPackageLocationFilter();

    @Test
    public void testImageNull() {
        boolean result = underTest.filterImage(null, mock(Image.class), null);

        assertFalse(result);
    }

    @Test
    public void testStackDetailIsNull() {
        boolean result = underTest.filterImage(mock(Image.class), mock(Image.class), null);

        assertFalse(result);
    }

    @Test
    public void testRepoIsNull() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", null, "1"));

        boolean result = underTest.filterImage(image, mock(Image.class), null);

        assertFalse(result);
    }

    @Test
    public void testStackIsNull() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(null, null), "1"));

        boolean result = underTest.filterImage(image, mock(Image.class), null);

        assertFalse(result);
    }

    @Test
    public void testCurrentImageNull() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of(), Map.of()), "1"));

        boolean result = underTest.filterImage(image, null, null);

        assertFalse(result);
    }

    @Test
    public void testCurrentImageOsTypeEmpty() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of(), Map.of()), "1"));
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn(" ");

        boolean result = underTest.filterImage(image, currentImage, null);

        assertFalse(result);
    }

    @Test
    public void testOsTypeMissing() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of(), Map.of()), "1"));
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn("redhat7");

        boolean result = underTest.filterImage(image, currentImage, null);

        assertFalse(result);
    }

    @Test
    public void testNotMatching() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of("redhat7", "http://random.org/asdf/"), Map.of()), "1"));
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn("redhat7");

        boolean result = underTest.filterImage(image, currentImage, null);

        assertFalse(result);
    }

    @Test
    public void testMatching() {
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of("redhat7", "http://archive.cloudera.com/asdf/"),
                Map.of()), "1"));
        Image currentImage = mock(Image.class);
        when(currentImage.getOsType()).thenReturn("redhat7");

        boolean result = underTest.filterImage(image, currentImage, null);

        assertTrue(result);
    }

}