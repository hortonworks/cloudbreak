package com.sequenceiq.freeipa.service.image;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ImageProviderFactoryTest {

    @Mock
    private FreeIpaImageProvider freeIpaImageProvider;

    @Mock
    private CoreImageProvider coreImageProvider;

    @InjectMocks
    private ImageProviderFactory victim;

    @Test
    public void shouldReturnFreeIpaImageProviderInCaseOfNullCatalog() {
        ImageProvider actual = victim.getImageProvider(null);

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    public void shouldReturnFreeIpaImageProviderInCaseOfEmptyCatalog() {
        ImageProvider actual = victim.getImageProvider("");

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    public void shouldReturnFreeIpaImageProviderInCaseOfUrlCatalog() {
        ImageProvider actual = victim.getImageProvider("http://someCatalog");

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    public void shouldReturnFreeIpaImageProviderInCaseOfJsonFileCatalog() {
        ImageProvider actual = victim.getImageProvider("someCatalog.json");

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    public void shouldReturnFreeIpaImageProviderInCaseOfNamedCatalog() {
        ImageProvider actual = victim.getImageProvider("someCatalog");

        assertEquals(coreImageProvider, actual);
    }
}