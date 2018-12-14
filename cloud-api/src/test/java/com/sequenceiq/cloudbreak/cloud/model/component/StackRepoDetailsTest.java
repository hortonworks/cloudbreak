package com.sequenceiq.cloudbreak.cloud.model.component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StackRepoDetailsTest {

    private static final String MAJOR_VERSION = "2.6";

    private static final String LONG_VERSION = "2.6.15.5";

    private static final String LONG_VERSION_WITH_SUB = "2.6.15.5-234";

    private StackRepoDetails undertest = new StackRepoDetails();

    @Test
    public void getMajorHdpVersion() {
        undertest.setHdpVersion(MAJOR_VERSION);
        String res = undertest.getMajorHdpVersion();
        assertEquals(MAJOR_VERSION, res);
    }

    @Test
    public void getMajorHdpVersionWithLong() {
        undertest.setHdpVersion(LONG_VERSION);
        String res = undertest.getMajorHdpVersion();
        assertEquals(MAJOR_VERSION, res);
    }

    @Test
    public void getMajorHdpVersionWithSub() {
        undertest.setHdpVersion(LONG_VERSION_WITH_SUB);
        String res = undertest.getMajorHdpVersion();
        assertEquals(MAJOR_VERSION, res);
    }

    @Test
    public void getMajorHdpVersionEmpty() {
        undertest.setHdpVersion("");
        String res = undertest.getMajorHdpVersion();
        assertEquals("", res);
    }

    @Test
    public void getMajorHdpVersionNull() {
        undertest.setHdpVersion(null);
        String res = undertest.getMajorHdpVersion();
        assertEquals("", res);
    }
}