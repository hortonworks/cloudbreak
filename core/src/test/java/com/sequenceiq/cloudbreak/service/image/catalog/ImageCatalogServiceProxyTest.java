package com.sequenceiq.cloudbreak.service.image.catalog;


import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogServiceProxyTest {

    @Mock
    private VersionBasedImageCatalogService versionBasedImageCatalogService;

    @Mock
    private AdvertisedImageCatalogService advertisedImageCatalogService;

    @InjectMocks
    private ImageCatalogServiceProxy victim;

    @Mock
    private CloudbreakImageCatalogV3 cloudbreakImageCatalogV3;

    @Mock
    private ImageFilter imageFilter;

    @Mock
    private Versions versions;

    @Test
    public void shouldRetrieveImagesFromVersionBasedImageCatalogServiceInCaseOfNonNullVersion() {
        when(cloudbreakImageCatalogV3.getVersions()).thenReturn(versions);

        victim.getImages(cloudbreakImageCatalogV3, imageFilter);

        verify(versionBasedImageCatalogService).getImages(cloudbreakImageCatalogV3, imageFilter);
        verifyNoInteractions(advertisedImageCatalogService);
    }

    @Test
    public void shouldRetrieveImagesFromAdvertisedImageCatalogServiceInCaseOfNullVersion() {
        victim.getImages(cloudbreakImageCatalogV3, imageFilter);

        verify(advertisedImageCatalogService).getImages(cloudbreakImageCatalogV3, imageFilter);
        verifyNoInteractions(versionBasedImageCatalogService);
    }

    @Test
    public void shouldRetrieveFilterResultFromVersionBasedImageCatalogServiceInCaseOfNonNullVersion() {
        when(cloudbreakImageCatalogV3.getVersions()).thenReturn(versions);

        victim.getImageFilterResult(cloudbreakImageCatalogV3);

        verify(versionBasedImageCatalogService).getImageFilterResult(cloudbreakImageCatalogV3);
        verifyNoInteractions(advertisedImageCatalogService);
    }

    @Test
    public void shouldRetrieveFilterResultFromAdvertisedImageCatalogServiceInCaseOfNullVersion() {
        victim.getImageFilterResult(cloudbreakImageCatalogV3);

        verify(advertisedImageCatalogService).getImageFilterResult(cloudbreakImageCatalogV3);
        verifyNoInteractions(versionBasedImageCatalogService);
    }

    @Test
    public void shouldValidateByVersionBasedImageCatalogServiceInCaseOfNonNullVersion() throws CloudbreakImageCatalogException {
        when(cloudbreakImageCatalogV3.getVersions()).thenReturn(versions);

        victim.validate(cloudbreakImageCatalogV3);

        verify(versionBasedImageCatalogService).validate(cloudbreakImageCatalogV3);
        verifyNoInteractions(advertisedImageCatalogService);
    }

    @Test
    public void shouldValidateByAdvertisedImageCatalogServiceInCaseOfNullVersion() throws CloudbreakImageCatalogException {
        victim.validate(cloudbreakImageCatalogV3);

        verify(advertisedImageCatalogService).validate(cloudbreakImageCatalogV3);
        verifyNoInteractions(versionBasedImageCatalogService);
    }
}