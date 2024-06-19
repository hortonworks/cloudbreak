package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@ExtendWith(MockitoExtension.class)
class ImageUtilTest {

    private ImageUtil underTest = new ImageUtil();

    @Mock
    private Image image;

    @Test
    public void testArm64ImageReturnsTrue() {
        when(image.getTags()).thenReturn(Map.of("platform", "arm64"));

        assertTrue(underTest.isArm64Image(image));
    }

    @Test
    public void testX86ImageReturnsTrue() {
        when(image.getTags()).thenReturn(Map.of("platform", "x86-64"));

        assertFalse(underTest.isArm64Image(image));
    }

    @Test
    public void testMissingPlatformTagReturnsFalse() {
        when(image.getTags()).thenReturn(Map.of());

        assertFalse(underTest.isArm64Image(image));
    }
}