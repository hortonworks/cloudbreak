package com.sequenceiq.cloudbreak.service.runtimes;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@RunWith(MockitoJUnitRunner.class)
public class SupportedRuntimesTest {

    private static final String RUNTIME_700 = "7.0.0";

    private static final String RUNTIME_710 = "7.1.0";

    private static final String RUNTIME_720 = "7.2.0";

    private static final String RUNTIME_7210 = "7.2.10";

    @InjectMocks
    private SupportedRuntimes underTest;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Test
    public void testAllowEverything() {
        Assert.assertTrue("Latest runtime is not configured, shall support everything", underTest.isSupported("does not matter"));
    }

    @Test
    public void testSupported() {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", RUNTIME_710);
        // Older or equal versions shall be supported
        Assert.assertTrue(underTest.isSupported(RUNTIME_710));
        Assert.assertTrue(underTest.isSupported("7.1.0.0"));
        Assert.assertTrue(underTest.isSupported("7.0.99.0"));
        Assert.assertTrue(underTest.isSupported("7"));
        Assert.assertTrue(underTest.isSupported("7.0.99"));
    }

    @Test
    public void testNotSupported() {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", RUNTIME_710);
        // Newer versions shall not be supported
        Assert.assertFalse(underTest.isSupported(RUNTIME_720));
    }

    @Test
    public void testInvalidVersions() {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", RUNTIME_710);
        // Invalid versions shall not be supported, but at least they shall not throw exception
        Assert.assertFalse(underTest.isSupported("blah"));
        Assert.assertFalse(underTest.isSupported("8"));
    }

    @Test
    public void testSupportedByFirstDefaultImageCatalogRuntime() throws CloudbreakImageCatalogException {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of(RUNTIME_7210, RUNTIME_700));

        Assert.assertTrue(underTest.isSupported(RUNTIME_710));
        Assert.assertTrue(underTest.isSupported(RUNTIME_720));
    }

    @Test
    public void testNotSupportedByFirstDefaultImageCatalogRuntime() throws CloudbreakImageCatalogException {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of(RUNTIME_700, RUNTIME_7210));

        Assert.assertFalse(underTest.isSupported(RUNTIME_710));
        Assert.assertFalse(underTest.isSupported(RUNTIME_720));
    }

    @Test
    public void testNotSupportedByDefaultImageCatalogRuntimes() throws CloudbreakImageCatalogException {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of(RUNTIME_710));

        Assert.assertFalse(underTest.isSupported(RUNTIME_720));
    }

    @Test
    public void testSupportInCaseOfEmptyImageCatalogResponse() throws CloudbreakImageCatalogException {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of());

        Assert.assertTrue(underTest.isSupported(RUNTIME_720));
    }

    @Test
    public void testSupportInCaseOfImageCatalogException() throws CloudbreakImageCatalogException {
        Whitebox.setInternalState(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenThrow(new CloudbreakImageCatalogException(""));

        Assert.assertTrue(underTest.isSupported(RUNTIME_720));
    }
}
