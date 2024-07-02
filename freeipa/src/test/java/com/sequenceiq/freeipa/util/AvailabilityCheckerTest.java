package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class AvailabilityCheckerTest {

    private static final String PACKAGE_NAME = "salt-bootstrap";

    private static final Versioned AFTER_VERSION = () -> "2.19.0";

    @InjectMocks
    private TestAvailbilityChecker underTest;

    @Mock
    private ImageService imageService;

    @Test
    void testAvailable() {
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
    void testUnavailable() {
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
    void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion("");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));

        stack.setAppVersion(" ");
        assertFalse(underTest.isAvailable(stack, AFTER_VERSION));
    }

    @Test
    void testPackageAvailable() {
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
    void testPackageUnavailable() {
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
    void testPackageVersionIsBlank() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);

        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, "");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));

        packageVersions.put(PACKAGE_NAME, " ");
        assertFalse(underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION));
    }

    @Test
    void testImageNotFoundForPackageVersion() {
        Stack stack = new Stack();
        when(imageService.getImageForStack(stack)).thenThrow(ImageNotFoundException.class);

        boolean result = underTest.isPackageAvailable(stack, PACKAGE_NAME, AFTER_VERSION);

        assertFalse(result);
    }

    @Test
    void testIsRequiredPackagesInstalledWhenRequiredPackagesIsNull() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);
        packageVersions.put(PACKAGE_NAME, "2.19.0");

        assertTrue(underTest.isRequiredPackagesInstalled(stack, null));
    }

    @Test
    void testIsRequiredPackagesInstalledWhenRequiredPackagesIsEmpty() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);
        packageVersions.put(PACKAGE_NAME, "2.19.0");

        assertTrue(underTest.isRequiredPackagesInstalled(stack, Set.of()));
    }

    @Test
    void testIsRequiredPackagesInstalledWhenRequiredPackagesAreAvailable() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);
        packageVersions.put(PACKAGE_NAME, "2.19.0");

        assertTrue(underTest.isRequiredPackagesInstalled(stack, Set.of(PACKAGE_NAME)));
    }

    @Test
    void testIsRequiredPackagesInstalledWhenRequiredPackagesAreUnvailable() {
        Stack stack = new Stack();
        Map<String, String> packageVersions = createPackageVersions(stack);
        packageVersions.put(PACKAGE_NAME, "2.19.0");

        assertFalse(underTest.isRequiredPackagesInstalled(stack, Set.of("unknown-package")));
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