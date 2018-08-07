package com.sequenceiq.cloudbreak.util;

import org.junit.Assert;
import org.junit.Test;

public class ClientVersionUtilTest {
    @Test
    public void checkVersionOk() {
        boolean result = ClientVersionUtil.checkVersion("2.4.0-dev.250", "2.4.0-rc.14");
        Assert.assertTrue(result);
    }

    @Test
    public void checkVersionOkWithV() {
        boolean result = ClientVersionUtil.checkVersion("2.4.0-dev.250", "v2.4.0-rc.14");
        Assert.assertTrue(result);
    }

    @Test
    public void checkVersionNok() {
        boolean result = ClientVersionUtil.checkVersion("2.5.0-dev.250", "2.4.0-rc.14");
        Assert.assertFalse(result);
    }

    @Test
    public void checkVersionNokTooShort() {
        boolean result = ClientVersionUtil.checkVersion("2", "2.4.0-rc.14");
        Assert.assertFalse(result);
    }

    @Test
    public void checkVersionNokUndefined() {
        boolean result = ClientVersionUtil.checkVersion("undefined", "2.4.0-rc.14");
        Assert.assertFalse(result);
    }

    @Test
    public void checkVersionNokEmpty() {
        boolean result = ClientVersionUtil.checkVersion("", "2.4.0-rc.14");
        Assert.assertFalse(result);
    }

    @Test
    public void checkVersionNokNull() {
        boolean result = ClientVersionUtil.checkVersion(null, "2.4.0-rc.14");
        Assert.assertFalse(result);
    }

    @Test
    public void checkVersionOkSnapshotGoesThrough() {
        boolean result = ClientVersionUtil.checkVersion("2.5.0-dev.250", "snapshot-2.4.0-rc.14");
        Assert.assertTrue(result);
    }

}