package com.sequenceiq.cloudbreak.core.bootstrap.service;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.PackageVersionChecker;

class SaltBootstrapVersionCheckerTest {

    private final SaltBootstrapVersionChecker underTest = new SaltBootstrapVersionChecker();

    @Test
    public void testRestartNeededFlagSupported() {
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.4")));
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.4-134")));
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.5")));
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.5-3245")));
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("0.14")));
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("0.14.0")));
        assertTrue(underTest.isRestartNeededFlagSupported(getJsonImage("1.0.0")));
    }

    @Test
    public void testRestartNeededFlagNotSupported() {
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.2")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.2-134")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.1")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.0")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.1.1")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.13.1-134")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.12.3")));
        assertFalse(underTest.isRestartNeededFlagSupported(getJsonImage("0.12.3-3245")));
    }

    @Test
    public void testFingerPrintSupported() {
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("0.13.2")));
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("0.13.2-134")));
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("0.13.3")));
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("0.13.3-3245")));
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("0.14")));
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("0.14.0")));
        assertTrue(underTest.isFingerprintingSupported(getJsonImage("1.0.0")));
    }

    @Test
    public void testFingerPrintNotSupported() {
        assertFalse(underTest.isFingerprintingSupported(getJsonImage("0.13.1")));
        assertFalse(underTest.isFingerprintingSupported(getJsonImage("0.13.0")));
        assertFalse(underTest.isFingerprintingSupported(getJsonImage("0.13.1.1")));
        assertFalse(underTest.isFingerprintingSupported(getJsonImage("0.13.1-134")));
        assertFalse(underTest.isFingerprintingSupported(getJsonImage("0.12.3")));
        assertFalse(underTest.isFingerprintingSupported(getJsonImage("0.12.3-3245")));
    }

    @Test
    public void testNullImage() {
        assertFalse(underTest.isFingerprintingSupported(null));
    }

    @Test
    public void testNoPackageVersionMap() {
        Image image = new Image(null, null, null, null, null, null, null, null, null, null, null, null);
        assertFalse(underTest.isFingerprintingSupported(new Json(image)));
    }

    @Test
    public void testNoPackageVersionForSb() {
        Image image = new Image(null, null, null, null, null, null, null, null, Map.of(), null, null, null);
        assertFalse(underTest.isFingerprintingSupported(new Json(image)));
    }

    protected Json getJsonImage(String version) {
        Image image = new Image(null, null, null, null, null, null, null, null, Map.of(PackageVersionChecker.SALT_BOOTSTRAP, version), null, null, null);
        return new Json(image);
    }
}