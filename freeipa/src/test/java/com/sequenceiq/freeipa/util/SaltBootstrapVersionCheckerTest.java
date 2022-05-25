package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class SaltBootstrapVersionCheckerTest {

    @Mock
    private ImageService imageService;

    @Mock
    private Stack stack;

    @Mock
    private Image image;

    @InjectMocks
    private SaltBootstrapVersionChecker underTest;

    @BeforeEach
    void setUp() {
        when(imageService.getImageForStack(stack)).thenReturn(image);
    }

    @Test
    void isChangeSaltuserPasswordSupportedTrue() {
        setSaltBootstrapVersion("0.13.6-2022-05-31T16:13:05");

        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertTrue(result);
    }

    @Test
    void isChangeSaltuserPasswordSupportedFalse() {
        setSaltBootstrapVersion("0.13.5-2022-05-31T16:13:05");

        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertFalse(result);
    }

    @Test
    void isChangeSaltuserPasswordSupportedFalseWithoutPackageVersion() {
        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertFalse(result);
    }

    @Test
    void isChangeSaltuserPasswordSupportedFalseWithoutImage() {
        when(imageService.getImageForStack(stack)).thenReturn(null);

        boolean result = underTest.isChangeSaltuserPasswordSupported(stack);

        assertFalse(result);
    }

    private void setSaltBootstrapVersion(String version) {
        when(image.getPackageVersions()).thenReturn(Map.of(SaltBootstrapVersionChecker.SALT_BOOTSTRAP_PACKACE, version));
    }

}