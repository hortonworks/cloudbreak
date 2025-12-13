package com.sequenceiq.freeipa.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageProviderFactoryTest {

    @Mock
    private FreeIpaImageProvider freeIpaImageProvider;

    @Mock
    private CoreImageProvider coreImageProvider;

    @InjectMocks
    private ImageProviderFactory victim;

    @Test
    void shouldReturnFreeIpaImageProviderInCaseOfNullCatalog() {
        ImageProvider actual = victim.getImageProvider(null);

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    void shouldReturnFreeIpaImageProviderInCaseOfEmptyCatalog() {
        ImageProvider actual = victim.getImageProvider("");

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    void shouldReturnFreeIpaImageProviderInCaseOfUrlCatalog() {
        ImageProvider actual = victim.getImageProvider("http://someCatalog");

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    void shouldReturnFreeIpaImageProviderInCaseOfJsonFileCatalog() {
        ImageProvider actual = victim.getImageProvider("someCatalog.json");

        assertEquals(freeIpaImageProvider, actual);
    }

    @Test
    void shouldReturnFreeIpaImageProviderInCaseOfNamedCatalog() {
        ImageProvider actual = victim.getImageProvider("someCatalog");

        assertEquals(coreImageProvider, actual);
    }
}