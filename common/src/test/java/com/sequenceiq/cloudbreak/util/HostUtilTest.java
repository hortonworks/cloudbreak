package com.sequenceiq.cloudbreak.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HostUtilTest {

    @Test
    public void testHasHostWhenEndWithPortWithoutSlash() {
        boolean actual = HostUtil.hasPort("http://asdasd:9999");
        Assertions.assertTrue(actual);
    }

    @Test
    public void testHasHostWhenEndWithPortFiveCharsWithoutSlash() {
        boolean actual = HostUtil.hasPort("http://asdasd:99999");
        Assertions.assertTrue(actual);
    }

    @Test
    public void testHasHostWhenEndWithPortWithSlash() {
        boolean actual = HostUtil.hasPort("http://asdasd:9999/");
        Assertions.assertTrue(actual);
    }

    @Test
    public void testHasHostWhenEndWithPortWithContext() {
        boolean actual = HostUtil.hasPort("http://asdasd:9999/asd");
        Assertions.assertTrue(actual);
    }

    @Test
    public void testHasHostWhenEndWithPortWithContextAndSlash() {
        boolean actual = HostUtil.hasPort("http://asdasd:9999/asd/");
        Assertions.assertTrue(actual);
    }

    @Test
    public void testHasHostWhenEndWithoutPort() {
        boolean actual = HostUtil.hasPort("http://asdasd");
        Assertions.assertFalse(actual);
    }

    @Test
    public void testHasHostWhenEndWithoutPortWithSlash() {
        boolean actual = HostUtil.hasPort("http://asdasd/");
        Assertions.assertFalse(actual);
    }

    @Test
    public void testHasHostWhenEndWithoutPortAndWithContext() {
        boolean actual = HostUtil.hasPort("http://asdasd/asd");
        Assertions.assertFalse(actual);
    }

    @Test
    public void testHasHostWhenEndWithoutPortAndWithContextAndSlash() {
        boolean actual = HostUtil.hasPort("http://asdasd/asd/");
        Assertions.assertFalse(actual);
    }
}
