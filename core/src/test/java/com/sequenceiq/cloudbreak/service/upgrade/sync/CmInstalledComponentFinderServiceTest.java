package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;

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
        Set<Image> candidateImages = Set.of(mock(Image.class));
        Set<ClouderaManagerRepo> candidateCmRepos = Set.of();
        ClouderaManagerRepo chosenClouderaManagerRepo = new ClouderaManagerRepo().withVersion(CM_VERSION);
        Component cmRepoComponent = new Component(ComponentType.CM_REPO_DETAILS, "cmRepoDetails", new Json("{}"), stack);
        when(imageReaderService.getCmRepos(eq(candidateImages))).thenReturn(candidateCmRepos);
        when(cmInfoRetriever.queryCmVersion(eq(stack))).thenReturn(CM_VERSION);
        when(cmProductChooserService.chooseCmRepo(eq(CM_VERSION), eq(candidateCmRepos))).thenReturn(Optional.of(chosenClouderaManagerRepo));
        when(componentConverter.fromClouderaManagerRepo(chosenClouderaManagerRepo, stack)).thenReturn(cmRepoComponent);

        Optional<Component> foundComponentOptional = underTest.findCmRepoComponent(stack, candidateImages);

        assertTrue(foundComponentOptional.isPresent());
        assertThat(foundComponentOptional.get(), hasProperty("componentType", is(ComponentType.CM_REPO_DETAILS)));
        verify(imageReaderService).getCmRepos(eq(candidateImages));
        verify(cmInfoRetriever).queryCmVersion(eq(stack));
        verify(cmProductChooserService).chooseCmRepo(eq(CM_VERSION), eq(candidateCmRepos));
        verify(componentConverter).fromClouderaManagerRepo(eq(chosenClouderaManagerRepo), eq(stack));
    }

    @Test
    void testFindParcelComponents() {
        Set<Image> candidateImages = Set.of(mock(Image.class));
        Set<ClouderaManagerProduct> candidateCmProduct = Set.of();
        Set<ParcelInfo> queriedParcelInfo = Set.of();
        Set<ClouderaManagerProduct> chosenClouderaManagerProducts = Set.of();
        Set<Component> chosenComponents = Set.of(new Component(ComponentType.CDH_PRODUCT_DETAILS, "cdhProductDetails", new Json("{}"), stack));
        when(imageReaderService.getParcels(eq(candidateImages), anyBoolean())).thenReturn(candidateCmProduct);
        when(cmInfoRetriever.queryActiveParcels(eq(stack))).thenReturn(queriedParcelInfo);
        when(cmProductChooserService.chooseParcelProduct(queriedParcelInfo, candidateCmProduct)).thenReturn(chosenClouderaManagerProducts);
        when(componentConverter.fromClouderaManagerProductList(chosenClouderaManagerProducts, stack)).thenReturn(chosenComponents);

        Set<Component> resultSet = underTest.findParcelComponents(stack, candidateImages);

        assertThat(resultSet, hasSize(1));
        assertThat(resultSet, contains(hasProperty("componentType", is(ComponentType.CDH_PRODUCT_DETAILS))));
        verify(imageReaderService).getParcels(eq(candidateImages), anyBoolean());
        verify(cmInfoRetriever).queryActiveParcels(eq(stack));
        verify(cmProductChooserService).chooseParcelProduct(eq(queriedParcelInfo), eq(candidateCmProduct));
        verify(componentConverter).fromClouderaManagerProductList(eq(chosenClouderaManagerProducts), eq(stack));
    }

}
