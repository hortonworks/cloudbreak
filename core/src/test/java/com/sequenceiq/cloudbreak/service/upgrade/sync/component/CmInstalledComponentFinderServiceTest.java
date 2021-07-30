package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;

@ExtendWith(MockitoExtension.class)
public class CmInstalledComponentFinderServiceTest {

    public static final String CM_VERSION = "cmVersion";

    @Mock
    private CmProductChooserService cmProductChooserService;

    @Mock
    private CmServerQueryService cmInfoRetriever;

    @Mock
    private ImageReaderService imageReaderService;

    @InjectMocks
    private CmInstalledComponentFinderService underTest;

    @Mock
    private Stack stack;

    @Test
    void testFindCmRepoComponent() {
        Set<Image> candidateImages = Set.of(mock(Image.class));
        Set<ClouderaManagerRepo> candidateCmRepos = Set.of();
        ClouderaManagerRepo chosenClouderaManagerRepo = new ClouderaManagerRepo().withVersion(CM_VERSION);
        when(imageReaderService.getCmRepos(eq(candidateImages))).thenReturn(candidateCmRepos);
        when(cmInfoRetriever.queryCmVersion(eq(stack))).thenReturn(Optional.of(CM_VERSION));
        when(cmProductChooserService.chooseCmRepo(eq(Optional.of(CM_VERSION)), eq(candidateCmRepos))).thenReturn(Optional.of(chosenClouderaManagerRepo));

        CmRepoSyncOperationResult cmRepoSyncOperationResult = underTest.findCmRepoComponent(stack, candidateImages);

        assertTrue(cmRepoSyncOperationResult.getFoundClouderaManagerRepo().isPresent());
        verify(imageReaderService).getCmRepos(eq(candidateImages));
        verify(cmInfoRetriever).queryCmVersion(eq(stack));
        verify(cmProductChooserService).chooseCmRepo(eq(Optional.of(CM_VERSION)), eq(candidateCmRepos));
    }

    @Test
    void testFindParcelComponents() {
        Set<Image> candidateImages = Set.of(mock(Image.class));
        Set<ClouderaManagerProduct> candidateCmProduct = Set.of(new ClouderaManagerProduct());
        Set<ParcelInfo> queriedParcelInfo = Set.of(new ParcelInfo("", ""));
        Set<ClouderaManagerProduct> chosenClouderaManagerProducts = Set.of(new ClouderaManagerProduct());
        when(stack.isDatalake()).thenReturn(true);
        when(imageReaderService.getParcels(candidateImages, true)).thenReturn(candidateCmProduct);
        when(cmInfoRetriever.queryActiveParcels(eq(stack))).thenReturn(queriedParcelInfo);
        when(cmProductChooserService.chooseParcelProduct(queriedParcelInfo, candidateCmProduct)).thenReturn(chosenClouderaManagerProducts);

        CmParcelSyncOperationResult cmParcelSyncOperationResult = underTest.findParcelComponents(stack, candidateImages);

        assertThat(cmParcelSyncOperationResult.getInstalledParcels(), hasSize(1));
        verify(imageReaderService).getParcels(candidateImages, true);
        verify(cmInfoRetriever).queryActiveParcels(stack);
        verify(cmProductChooserService).chooseParcelProduct(queriedParcelInfo, candidateCmProduct);
    }

}
