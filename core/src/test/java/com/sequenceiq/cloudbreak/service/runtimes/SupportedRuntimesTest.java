package com.sequenceiq.cloudbreak.service.runtimes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class SupportedRuntimesTest {

    private SupportedRuntimes underTest;

    @Before
    public void setUp() {
        underTest = new SupportedRuntimes();
    }

    @Test
    public void testAllowEverything() {
        Assert.assertTrue("Latest runtime is not configured, shall support everything", underTest.isSupported("does not matter"));
    }

    @Test
    public void testSupported() {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "7.1.0");
        // Older or equal versions shall be supported
        Assert.assertTrue(underTest.isSupported("7.1.0"));
        Assert.assertTrue(underTest.isSupported("7.1.0.0"));
        Assert.assertTrue(underTest.isSupported("7.0.99.0"));
        Assert.assertTrue(underTest.isSupported("7"));
        Assert.assertTrue(underTest.isSupported("7.0.99"));
    }

    @Test
    public void testNotSupported() {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "7.1.0");
        // Newer versions shall not be supported
        Assert.assertFalse(underTest.isSupported("7.2.0"));
    }

    @Test
    public void testInvalidVersions() {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "7.1.0");
        // Invalid versions shall not be supported, but at least they shall not throw exception
        Assert.assertFalse(underTest.isSupported("blah"));
        Assert.assertFalse(underTest.isSupported("8"));
    }
}
