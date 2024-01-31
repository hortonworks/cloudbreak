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
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;

@ExtendWith(MockitoExtension.class)
class CdhPackageLocationFilterTest {

    @InjectMocks
    private CdhPackageLocationFilter underTest;

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
    public void testStackDetailIsNull() {
        assertFalse(underTest.filterImage(mock(Image.class), imageFilterParams));
    }

    @Test
    public void testRepoIsNull() {
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", null, "1"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testStackIsNull() {
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(null, null), "1"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testCurrentImageNull() {
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of(), Map.of()), "1"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testCurrentImageOsTypeEmpty() {
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of(), Map.of()), "1"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testOsTypeMissing() {
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of(), Map.of()), "1"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testNotMatching() {
        when(image.getStackDetails()).thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of("redhat7", "http://random.org/asdf/"), Map.of()), "1"));

        assertFalse(underTest.filterImage(image, imageFilterParams));
    }

    @Test
    public void testMatching() {
        when(image.getStackDetails())
                .thenReturn(new ImageStackDetails("1", new StackRepoDetails(Map.of("redhat7", "http://archive.cloudera.com/asdf/"), Map.of()), "1"));

        assertTrue(underTest.filterImage(image, imageFilterParams));
    }
}