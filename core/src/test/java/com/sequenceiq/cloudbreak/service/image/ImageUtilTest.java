package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
        when(image.getArchitecture()).thenReturn("arm64");

        assertTrue(underTest.isArm64Image(image));
    }

    @Test
    public void testX86ImageReturnsTrue() {
        when(image.getArchitecture()).thenReturn("x86-64");

        assertFalse(underTest.isArm64Image(image));
    }

    @Test
    public void testMissingArchitectureReturnsFalse() {
        when(image.getArchitecture()).thenReturn(null);

        assertFalse(underTest.isArm64Image(image));
    }

    @Test
    public void testEmptyArchitectureReturnsFalse() {
        when(image.getArchitecture()).thenReturn("");

        assertFalse(underTest.isArm64Image(image));
    }
}