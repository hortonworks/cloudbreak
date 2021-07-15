package com.sequenceiq.cloudbreak.service.upgrade.sync;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;

@ExtendWith(MockitoExtension.class)
public class ImageReaderServiceTest {

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageReaderService underTest;

    @Test
    void testGetCandidateParcelsWhenMultipleImagesThenProductTransformerIsCalledEachTime() {
        Set<StatedImage> candidateImages = Set.of(getStatedImage(), getStatedImage());
        when(clouderaManagerProductTransformer.transform(any(), eq(false)))
                .thenReturn(Set.of(new ClouderaManagerProduct()))
                .thenReturn(Set.of(new ClouderaManagerProduct()));

        Set<ClouderaManagerProduct> candidateProducts = underTest.getParcels(candidateImages, true);

        assertThat(candidateProducts, hasSize(2));
        verify(clouderaManagerProductTransformer, times(2)).transform(any(Image.class), eq(false));
    }

    @Test
    void testGetCandidateParcelsWhenNoImagesThenProductTransformerIsNotCalled() {
        Set<StatedImage> candidateImages = Set.of();

        Set<ClouderaManagerProduct> candidateProducts = underTest.getParcels(candidateImages, true);

        assertThat(candidateProducts, hasSize(0));
        verify(clouderaManagerProductTransformer, never()).transform(any(Image.class), anyBoolean());
    }

    @Test
    void testGetCandidateCmReposWhenMultipleImagesThenGetCmRepoIsCalledEachTime() throws CloudbreakImageCatalogException {
        Set<StatedImage> candidateImages = Set.of(getStatedImage(), getStatedImage());
        when(imageService.getClouderaManagerRepo(any(Image.class)))
                .thenReturn(Optional.of(new ClouderaManagerRepo()))
                .thenReturn(Optional.of(new ClouderaManagerRepo()));

        Set<ClouderaManagerRepo> candidateRepos = underTest.getCmRepos(candidateImages);

        assertThat(candidateRepos, hasSize(2));
        verify(imageService, times(2)).getClouderaManagerRepo(any(Image.class));
    }

    @Test
    void testGetCandidateCmReposWhenNoImagesThenGetCmRepoIsNotCalled() throws CloudbreakImageCatalogException {
        Set<StatedImage> candidateImages = Set.of();

        Set<ClouderaManagerRepo> candidateRepos = underTest.getCmRepos(candidateImages);

        assertThat(candidateRepos, empty());
        verify(imageService, never()).getClouderaManagerRepo(any(Image.class));
    }

    private StatedImage getStatedImage() {
        Image image = mock(Image.class);
        StatedImage statedImage = mock(StatedImage.class);
        when(statedImage.getImage()).thenReturn(image);
        return statedImage;
    }

}
