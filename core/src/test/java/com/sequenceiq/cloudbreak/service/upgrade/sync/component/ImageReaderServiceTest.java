package com.sequenceiq.cloudbreak.service.upgrade.sync.component;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;

@ExtendWith(MockitoExtension.class)
public class ImageReaderServiceTest {

    private static final long STACK_ID = 1L;

    private static final Long WORKSPACE_ID = 1L;

    private static final String PARCEL_NAME = "NIFI";

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private ImageService imageService;

    @Mock
    private DefaultClouderaManagerRepoService clouderaManagerRepoService;

    @InjectMocks
    private ImageReaderService underTest;

    @Test
    void testGetCandidateParcelsWhenMultipleImagesThenProductTransformerIsCalledEachTime() {
        Set<Image> candidateImages = Set.of(mock(Image.class), mock(Image.class));
        when(clouderaManagerProductTransformer.transform(any(), eq(true), eq(false)))
                .thenReturn(Set.of(new ClouderaManagerProduct()))
                .thenReturn(Set.of(new ClouderaManagerProduct()));

        Set<ClouderaManagerProduct> candidateProducts = underTest.getParcels(candidateImages, true);

        assertThat(candidateProducts, hasSize(2));
        verify(clouderaManagerProductTransformer, times(2)).transform(any(Image.class), eq(true), eq(false));
    }

    @Test
    void testGetCandidateParcelsWhenNoImagesThenProductTransformerIsNotCalled() {
        Set<Image> candidateImages = Set.of();

        Set<ClouderaManagerProduct> candidateProducts = underTest.getParcels(candidateImages, true);

        assertThat(candidateProducts, hasSize(0));
        verify(clouderaManagerProductTransformer, never()).transform(any(Image.class), anyBoolean(), anyBoolean());
    }

    @Test
    void testGetCandidateCmReposWhenMultipleImagesThenGetCmRepoIsCalledEachTime() throws CloudbreakImageCatalogException {
        Set<Image> candidateImages = Set.of(mock(Image.class), mock(Image.class));
        when(clouderaManagerRepoService.getClouderaManagerRepo(any(Image.class)))
                .thenReturn(Optional.of(new ClouderaManagerRepo()))
                .thenReturn(Optional.of(new ClouderaManagerRepo()));

        Set<ClouderaManagerRepo> candidateRepos = underTest.getCmRepos(candidateImages);

        assertThat(candidateRepos, hasSize(2));
        verify(clouderaManagerRepoService, times(2)).getClouderaManagerRepo(any(Image.class));
    }

    @Test
    void testGetCandidateCmReposWhenNoImagesThenGetCmRepoIsNotCalled() throws CloudbreakImageCatalogException {
        Set<Image> candidateImages = Set.of();

        Set<ClouderaManagerRepo> candidateRepos = underTest.getCmRepos(candidateImages);

        assertThat(candidateRepos, empty());
        verify(clouderaManagerRepoService, never()).getClouderaManagerRepo(any(Image.class));
    }

    @Test
    public void testGetPreWarmParcelNamesFromImageShouldReturnTheParcelNames() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = mock(Image.class);
        Set<ClouderaManagerProduct> products = Set.of(new ClouderaManagerProduct().withName(PARCEL_NAME));

        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenReturn(StatedImage.statedImage(currentImage, null, null));
        when(clouderaManagerProductTransformer.transform(currentImage, true, true)).thenReturn(products);

        Set<String> actual = underTest.getParcelNames(WORKSPACE_ID, STACK_ID);

        assertTrue(actual.contains(PARCEL_NAME));
        verify(imageService).getCurrentImage(WORKSPACE_ID, STACK_ID);
        verify(clouderaManagerProductTransformer).transform(currentImage, true, true);
    }

    @Test
    public void testGetPreWarmParcelNamesFromImageShouldReturnEmptySetWhenTheCurrentImageNotFound() throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenThrow(new CloudbreakImageCatalogException("error"));

        Set<String> actual = underTest.getParcelNames(WORKSPACE_ID, STACK_ID);

        assertTrue(actual.isEmpty());
        verify(imageService).getCurrentImage(WORKSPACE_ID, STACK_ID);
        verifyNoInteractions(clouderaManagerProductTransformer);
    }

}
