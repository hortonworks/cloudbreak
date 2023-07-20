package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerPackageLocationFilterTest {

    @InjectMocks
    private ClouderaManagerPackageLocationFilter underTest;

    @Mock
    private ImageFilterParams imageFilterParams;

    @Mock
    private Image image;

    @Test
    public void testImageNull() {
        assertFalse(underTest.filterImage(null, imageFilterParams));
    }

    @Test
    public void testRepoNull() {
        assertFalse(underTest.filterImage(mock(Image.class), imageFilterParams));
    }

    @Test
    public void testCurrentImageNull() {
        when(image.getRepo()).thenReturn(Map.of());

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testCurrentImageOsTypeEmpty() {
        when(image.getRepo()).thenReturn(Map.of());
        when(imageFilterParams.getCurrentImage()).thenReturn(createModelImage(" "));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testOsTypeMissing() {
        when(image.getRepo()).thenReturn(Map.of());
        when(imageFilterParams.getCurrentImage()).thenReturn(createModelImage("redhat7"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testNotMatching() {
        when(image.getRepo()).thenReturn(Map.of("redhat7", "http://random.org/asdf/"));
        when(imageFilterParams.getCurrentImage()).thenReturn(createModelImage("redhat7"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testMatching() {
        when(image.getRepo()).thenReturn(Map.of("redhat7", "http://archive.cloudera.com/asdf/"));
        when(imageFilterParams.getCurrentImage()).thenReturn(createModelImage("redhat7"));

        assertTrue(underTest.filterImage(image, imageFilterParams));
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createModelImage(String osType) {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(null, null, null, osType, null, null, null, null, null, null);
    }

}