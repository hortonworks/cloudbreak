package com.sequenceiq.common.api.util.versionchecker;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClientVersionUtilTest {
    @Test
    void checkVersionOk() {
        boolean result = ClientVersionUtil.checkVersion("2.4.0-b250", "2.4.0-b14");
        Assertions.assertTrue(result);
    }

    @Test
    void checkVersionOkMinorDifference() {
        boolean result = ClientVersionUtil.checkVersion("2.50.0-b250", "2.4.0-b14");
        Assertions.assertTrue(result);
    }

    @Test
    void checkVersionNokMajorDifference() {
        boolean result = ClientVersionUtil.checkVersion("3.5.0-b250", "2.4.0-b14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokUndefined() {
        boolean result = ClientVersionUtil.checkVersion("undefined", "2.4.0-b14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokEmpty() {
        boolean result = ClientVersionUtil.checkVersion("", "2.4.0-b14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokNull() {
        boolean result = ClientVersionUtil.checkVersion(null, "2.4.0-b14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionOkSnapshotGoesThrough() {
        boolean result = ClientVersionUtil.checkVersion("2.5.0-b250", "snapshot-2021-03-11T09:42:59");
        Assertions.assertTrue(result);
    }
}