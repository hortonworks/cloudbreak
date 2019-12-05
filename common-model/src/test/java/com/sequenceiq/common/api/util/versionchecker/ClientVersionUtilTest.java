package com.sequenceiq.common.api.util.versionchecker;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClientVersionUtilTest {
    @Test
    void checkVersionOk() {
        boolean result = ClientVersionUtil.checkVersion("2.4.0-dev.250", "2.4.0-rc.14");
        Assertions.assertTrue(result);
    }

    @Test
    void checkVersionOkWithV() {
        boolean result = ClientVersionUtil.checkVersion("2.4.0-dev.250", "v2.4.0-rc.14");
        Assertions.assertTrue(result);
    }

    @Test
    void checkVersionNok() {
        boolean result = ClientVersionUtil.checkVersion("2.5.0-dev.250", "2.4.0-rc.14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokTooShort() {
        boolean result = ClientVersionUtil.checkVersion("2", "2.4.0-rc.14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokUndefined() {
        boolean result = ClientVersionUtil.checkVersion("undefined", "2.4.0-rc.14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokEmpty() {
        boolean result = ClientVersionUtil.checkVersion("", "2.4.0-rc.14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionNokNull() {
        boolean result = ClientVersionUtil.checkVersion(null, "2.4.0-rc.14");
        Assertions.assertFalse(result);
    }

    @Test
    void checkVersionOkSnapshotGoesThrough() {
        boolean result = ClientVersionUtil.checkVersion("2.5.0-dev.250", "snapshot-2.4.0-rc.14");
        Assertions.assertTrue(result);
    }
}