package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogMetaData;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogWrapper;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogProviderTest {

    private static final String IMAGE_CATALOG_URL = "imageCatalogUrl";

    @Mock
    private CachedImageCatalogWrapperProvider cachedImageCatalogWrapperProvider;

    @InjectMocks
    private ImageCatalogProvider victim;

    @Test
    public void shouldReturnImageCatalogWithoutForceRefresh() throws CloudbreakImageCatalogException {
        ImageCatalogWrapper imageCatalogWrapper = mock(ImageCatalogWrapper.class);
        CloudbreakImageCatalogV3 imageCatalogV3 = mock(CloudbreakImageCatalogV3.class);
        when(cachedImageCatalogWrapperProvider.getImageCatalogWrapper(IMAGE_CATALOG_URL)).thenReturn(imageCatalogWrapper);
        when(imageCatalogWrapper.getImageCatalog()).thenReturn(imageCatalogV3);

        CloudbreakImageCatalogV3 actual = victim.getImageCatalogV3(IMAGE_CATALOG_URL);

        assertEquals(imageCatalogV3, actual);
        verifyNoMoreInteractions(cachedImageCatalogWrapperProvider);
    }

    @Test
    public void shouldReturnImageCatalogWithForceRefresh() throws CloudbreakImageCatalogException {
        ImageCatalogWrapper imageCatalogWrapper = mock(ImageCatalogWrapper.class);
        CloudbreakImageCatalogV3 imageCatalogV3 = mock(CloudbreakImageCatalogV3.class);
        when(cachedImageCatalogWrapperProvider.getImageCatalogWrapper(IMAGE_CATALOG_URL)).thenReturn(imageCatalogWrapper);
        when(imageCatalogWrapper.getImageCatalog()).thenReturn(imageCatalogV3);

        CloudbreakImageCatalogV3 actual = victim.getImageCatalogV3(IMAGE_CATALOG_URL, true);

        assertEquals(imageCatalogV3, actual);
        verify(cachedImageCatalogWrapperProvider).evictImageCatalogCache(IMAGE_CATALOG_URL);
    }

    @Test
    public void shouldReturnImageCatalogMetaData() throws CloudbreakImageCatalogException {
        ImageCatalogWrapper imageCatalogWrapper = mock(ImageCatalogWrapper.class);
        ImageCatalogMetaData imageCatalogMetaData = mock(ImageCatalogMetaData.class);
        when(cachedImageCatalogWrapperProvider.getImageCatalogWrapper(IMAGE_CATALOG_URL)).thenReturn(imageCatalogWrapper);
        when(imageCatalogWrapper.getImageCatalogMetaData()).thenReturn(imageCatalogMetaData);

        ImageCatalogMetaData actual = victim.getImageCatalogMetaData(IMAGE_CATALOG_URL);

        assertEquals(imageCatalogMetaData, actual);
        verifyNoMoreInteractions(cachedImageCatalogWrapperProvider);
    }
}