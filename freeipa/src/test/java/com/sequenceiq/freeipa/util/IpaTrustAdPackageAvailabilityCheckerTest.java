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
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class IpaTrustAdPackageAvailabilityCheckerTest {

    private static final Long STACK_ID = 123L;

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private IpaTrustAdPackageAvailabilityChecker underTest;

    private Stack stack;

    @Mock
    private Image image;

    @BeforeEach
    void init() {
        stack = new Stack();
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(imageService.getImageForStack(stack)).thenReturn(image);
    }

    @Test
    void testPackagageAvailable() {
        when(image.getPackageVersions()).thenReturn(Map.of("ipa-server-trust-ad", "4.9.13"));

        assertTrue(underTest.isPackageAvailable(STACK_ID));
    }

    @Test
    void testPackagageMissing() {
        when(image.getPackageVersions()).thenReturn(Map.of());

        assertFalse(underTest.isPackageAvailable(STACK_ID));
    }

}