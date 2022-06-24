package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;

@RunWith(MockitoJUnitRunner.class)
public class AvailabilityCheckerTest {

    private static final String PACKAGE_NAME = "salt-bootstrap";

    private static final Versioned AFTER_VERSION = () -> "2.19.0";

    @InjectMocks
    private TestAvailbilityChecker underTest;

    @Mock
    private ImageService imageService;

    @Test
    public void testAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0-rc.1");
        assertTrue(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.21.0-rc.1");
        assertTrue(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.20.0");
        assertTrue(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.20.0-dev.2");
        assertTrue(underTest.isAvailable(stack, AFTER_VERSION));
    }

    @Test
    public void testUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.19.0");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.19.0-rc.23");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.19.0-rc.122");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.19.0-dev.23");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.18.0");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("2.18.0-rc.2");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));
    }

    @Test
    public void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion(" ");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));
    }

    @Test
    public void testPackageAvailable() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);

        packageVersions.put(PACKAGE_NAME, "2.20.0-rc.1");
        assertTrue(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.21.0-rc.1");
        assertTrue(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.20.0");
        assertTrue(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.20.0-dev.2");
        assertTrue(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));
    }

    @Test
    public void testPackageUnavailable() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);

        packageVersions.put(PACKAGE_NAME, "2.19.0");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.19.0-rc.23");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.19.0-rc.122");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.19.0-dev.23");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.18.0");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "2.18.0-rc.2");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));
    }

    @Test
    public void testPackageVersionIsBlank() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);

        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, " ");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));
    }

    @Test
    public void testImageNotFoundForPackageVersion() {
        Stack stack = new Stack();
        when(imageService.getImageForStack(stack)).thenThrow(ImageNotFoundException.class);

        boolean result = underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION);

        assertFalse(result);
    }

    private Map<String, String> createPackageVersions(Stack stack) {
        Image image = mock(Image.class);
        when(imageService.getImageForStack(stack)).thenReturn(image);
        Map<String, String> packageVersions = new HashMap<>();
        when(image.getPackageVersions()).thenReturn(packageVersions);
        return packageVersions;
    }

    static class TestAvailbilityChecker extends AvailabilityChecker {

        public boolean isAvailable(Stack stack, Versioned versioned) {
            return super.isAvailable(stack, versioned);
        }

        @Override
        public boolean isPackageAvailable(Stack stack, String packageName, Versioned version) {
            return super.isPackageAvailable(stack, packageName, version);
        }
    }
}