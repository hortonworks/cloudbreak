package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    public void setUp() {
        lenient().when(image.getOsType()).thenReturn("redhat7");
    }

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

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testOsTypeMissing() {
        when(image.getRepo()).thenReturn(Map.of());

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testNotMatching() {
        when(image.getRepo()).thenReturn(Map.of("redhat7", "http://random.org/asdf/"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testMatching() {
        when(image.getRepo()).thenReturn(Map.of("redhat7", "http://archive.cloudera.com/asdf/"));

        assertTrue(underTest.filterImage(image, imageFilterParams));
    }

}