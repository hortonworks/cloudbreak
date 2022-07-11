package com.sequenceiq.cloudbreak.service.runtimes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@ExtendWith(MockitoExtension.class)
class SupportedRuntimesTest {

    private static final String RUNTIME_700 = "7.0.0";

    private static final String RUNTIME_710 = "7.1.0";

    private static final String RUNTIME_720 = "7.2.0";

    private static final String RUNTIME_7210 = "7.2.10";

    @InjectMocks
    private SupportedRuntimes underTest;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Test
    void testAllowEverything() {
        assertTrue(underTest.isSupported("does not matter"), "Latest runtime is not configured, shall support everything");
    }

    @Test
    void testSupported() {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", RUNTIME_710);
        // Older or equal versions shall be supported
        assertTrue(underTest.isSupported(RUNTIME_710));
        assertTrue(underTest.isSupported("7.1.0.0"));
        assertTrue(underTest.isSupported("7.0.99.0"));
        assertTrue(underTest.isSupported("7"));
        assertTrue(underTest.isSupported("7.0.99"));
    }

    @Test
    void testNotSupported() {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", RUNTIME_710);
        // Newer versions shall not be supported
        assertFalse(underTest.isSupported(RUNTIME_720));
    }

    @Test
    void testInvalidVersions() {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", RUNTIME_710);
        // Invalid versions shall not be supported, but at least they shall not throw exception
        assertFalse(underTest.isSupported("blah"));
        assertFalse(underTest.isSupported("8"));
    }

    @Test
    void testSupportedByFirstDefaultImageCatalogRuntime() throws CloudbreakImageCatalogException {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of(RUNTIME_7210, RUNTIME_700));

        assertTrue(underTest.isSupported(RUNTIME_710));
        assertTrue(underTest.isSupported(RUNTIME_720));
    }

    @Test
    void testNotSupportedByFirstDefaultImageCatalogRuntime() throws CloudbreakImageCatalogException {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of(RUNTIME_700, RUNTIME_7210));

        assertFalse(underTest.isSupported(RUNTIME_710));
        assertFalse(underTest.isSupported(RUNTIME_720));
    }

    @Test
    void testNotSupportedByDefaultImageCatalogRuntimes() throws CloudbreakImageCatalogException {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of(RUNTIME_710));

        assertFalse(underTest.isSupported(RUNTIME_720));
    }

    @Test
    void testSupportInCaseOfEmptyImageCatalogResponse() throws CloudbreakImageCatalogException {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(List.of());

        assertTrue(underTest.isSupported(RUNTIME_720));
    }

    @Test
    void testSupportInCaseOfImageCatalogException() throws CloudbreakImageCatalogException {
        ReflectionTestUtils.setField(underTest, "latestSupportedRuntime", "");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenThrow(new CloudbreakImageCatalogException(""));

        assertTrue(underTest.isSupported(RUNTIME_720));
    }
}
