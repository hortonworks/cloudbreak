package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@ExtendWith(MockitoExtension.class)
public class CmInstalledComponentFinderServiceTest {

    public static final String CM_VERSION = "cmVersion";

    @Mock
    private CmProductChooserService cmProductChooserService;

    @Mock
    private ComponentConverter componentConverter;

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
        Set<StatedImage> candidateImages = Set.of();
        Set<ClouderaManagerRepo> candidateCmRepos = Set.of();
        ClouderaManagerRepo chosenClouderaManagerRepo = new ClouderaManagerRepo().withVersion(CM_VERSION);
        Component cmRepoComponent = new Component(ComponentType.CM_REPO_DETAILS, "cmRepoDetails", new Json("{}"), stack);
        Set<Component> chosenComponents = new HashSet<>();
        when(imageReaderService.getCmRepos(eq(candidateImages))).thenReturn(candidateCmRepos);
        when(cmInfoRetriever.queryCmVersion(eq(stack))).thenReturn(CM_VERSION);
        when(cmProductChooserService.chooseCmRepo(eq(CM_VERSION), eq(candidateCmRepos))).thenReturn(Optional.of(chosenClouderaManagerRepo));
        when(componentConverter.fromClouderaManagerRepo(chosenClouderaManagerRepo, stack)).thenReturn(cmRepoComponent);

        underTest.findCmRepoComponent(stack, candidateImages, chosenComponents);

        assertThat(chosenComponents, hasSize(1));
        assertThat(chosenComponents, contains(hasProperty("componentType", is(ComponentType.CM_REPO_DETAILS))));
        verify(imageReaderService).getCmRepos(eq(candidateImages));
        verify(cmInfoRetriever).queryCmVersion(eq(stack));
        verify(cmProductChooserService).chooseCmRepo(eq(CM_VERSION), eq(candidateCmRepos));
        verify(componentConverter).fromClouderaManagerRepo(eq(chosenClouderaManagerRepo), eq(stack));
    }

    @Test
    void testFindParcelComponents() {
        Set<StatedImage> candidateImages = Set.of();
        Set<ClouderaManagerProduct> candidateCmProduct = Set.of();
        Set<ParcelInfo> queriedParcelInfo = Set.of();
        Set<ClouderaManagerProduct> chosenClouderaManagerProducts = Set.of();
        Set<Component> chosenComponents = Set.of(new Component(ComponentType.CDH_PRODUCT_DETAILS, "cdhProductDetails", new Json("{}"), stack));
        Set<Component> resultSet = new HashSet<>();
        when(imageReaderService.getParcels(eq(candidateImages), anyBoolean())).thenReturn(candidateCmProduct);
        when(cmInfoRetriever.queryActiveParcels(eq(stack))).thenReturn(queriedParcelInfo);
        when(cmProductChooserService.chooseParcelProduct(queriedParcelInfo, candidateCmProduct)).thenReturn(chosenClouderaManagerProducts);
        when(componentConverter.fromClouderaManagerProductList(chosenClouderaManagerProducts, stack)).thenReturn(chosenComponents);

        underTest.findParcelComponents(stack, candidateImages, resultSet);

        assertThat(resultSet, hasSize(1));
        assertThat(resultSet, contains(hasProperty("componentType", is(ComponentType.CDH_PRODUCT_DETAILS))));
        verify(imageReaderService).getParcels(eq(candidateImages), anyBoolean());
        verify(cmInfoRetriever).queryActiveParcels(eq(stack));
        verify(cmProductChooserService).chooseParcelProduct(eq(queriedParcelInfo), eq(candidateCmProduct));
        verify(componentConverter).fromClouderaManagerProductList(eq(chosenClouderaManagerProducts), eq(stack));
    }

}
