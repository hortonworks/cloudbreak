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
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CustomParcelFilterService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class CmInstalledComponentFinderServiceTest {

    public static final String CM_VERSION = "cmVersion";

    @Mock
    private CmProductChooserService cmProductChooserService;

    @Mock
    private CmServerQueryService cmInfoRetriever;

    @Mock
    private ImageReaderService imageReaderService;

    @Mock
    private CustomParcelFilterService customParcelFilterService;

    @InjectMocks
    private CmInstalledComponentFinderService underTest;

    @Mock
    private Stack stack;

    @Test
    void findsExplicitCmRepoWithoutCatalogCandidates() {
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion("7.13.2").withBuildNumber("12345").withBaseUrl("https://explicit/cm");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base", OsType.RHEL8, "x86_64", null, Set.of(), repo);
        ReflectionTestUtils.setField(underTest, "cmProductChooserService", new CmProductChooserService());
        when(cmInfoRetriever.queryCmVersion(stack)).thenReturn(Optional.of(repo.getFullVersion()));

        CmRepoSyncOperationResult result = underTest.findCmRepoComponent(stack, Set.of(), properties);

        org.assertj.core.api.Assertions.assertThat(result.getFoundClouderaManagerRepo()).contains(repo);
    }

    @Test
    void missingTargetCmRepoDoesNotHideCatalogCandidates() {
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion("7.13.2").withBuildNumber("12345");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        ReflectionTestUtils.setField(underTest, "cmProductChooserService", new CmProductChooserService());
        when(imageReaderService.getCmRepos(Set.of())).thenReturn(Set.of(repo));
        when(cmInfoRetriever.queryCmVersion(stack)).thenReturn(Optional.of(repo.getFullVersion()));

        CmRepoSyncOperationResult result = underTest.findCmRepoComponent(stack, Set.of(), properties);

        org.assertj.core.api.Assertions.assertThat(result.getFoundClouderaManagerRepo()).contains(repo);
    }

    @Test
    void findsExplicitAdditionalParcelOnDatalakeWithoutCatalogCandidates() {
        ClouderaManagerProduct parcel = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20").withParcel("https://explicit/flink");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base", OsType.RHEL8, "x86_64", null, Set.of(parcel), null);
        Set<ParcelInfo> active = Set.of(new ParcelInfo("FLINK", "1.20", ParcelStatus.ACTIVATED));
        ReflectionTestUtils.setField(underTest, "cmProductChooserService", new CmProductChooserService());
        when(stack.isDatalake()).thenReturn(true);
        when(cmInfoRetriever.queryActiveParcels(stack)).thenReturn(active);
        when(customParcelFilterService.filterCustomParcels(active, Set.of(parcel))).thenReturn(active);

        CmParcelSyncOperationResult result = underTest.findParcelComponents(stack, Set.of(), properties);

        org.assertj.core.api.Assertions.assertThat(result.getFoundCmProducts()).containsExactly(parcel);
    }

    @Test
    void findsOriginalParcelStillActiveAfterFailedExplicitUpgrade() {
        ClouderaManagerProduct target = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20.0-custom.2")
                .withParcel("https://internal.example/target/parcels/");
        ClouderaManagerProduct original = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20.0-custom.1")
                .withParcel("https://internal.example/original/parcels/");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withCurrentProducts(
                ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "base", OsType.RHEL8, "x86_64", null, Set.of(target), null),
                Set.of(original), null);
        ReflectionTestUtils.setField(underTest, "cmProductChooserService", new CmProductChooserService());
        ReflectionTestUtils.setField(underTest, "customParcelFilterService", new CustomParcelFilterService());
        when(cmInfoRetriever.queryActiveParcels(stack)).thenReturn(Set.of(new ParcelInfo("FLINK", "1.20.0-custom.1", ParcelStatus.ACTIVATED)));

        CmParcelSyncOperationResult result = underTest.findParcelComponents(stack, Set.of(), properties);

        org.assertj.core.api.Assertions.assertThat(result.getFoundCmProducts()).containsExactly(original);
    }

    @Test
    void findsOriginalCmRepoStillActiveAfterFailedExplicitUpgrade() {
        ClouderaManagerRepo target = new ClouderaManagerRepo().withVersion("7.13.2").withBuildNumber("2").withBaseUrl("https://target/cm/");
        ClouderaManagerRepo original = new ClouderaManagerRepo().withVersion("7.13.1").withBuildNumber("1").withBaseUrl("https://original/cm/");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withCurrentProducts(
                ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "base", OsType.RHEL8, "x86_64", null, Set.of(), target),
                Set.of(), original);
        ReflectionTestUtils.setField(underTest, "cmProductChooserService", new CmProductChooserService());
        when(cmInfoRetriever.queryCmVersion(stack)).thenReturn(Optional.of(original.getFullVersion()));

        CmRepoSyncOperationResult result = underTest.findCmRepoComponent(stack, Set.of(), properties);

        org.assertj.core.api.Assertions.assertThat(result.getFoundClouderaManagerRepo()).contains(original);
    }

    @Test
    void testFindCmRepoComponent() {
        Set<Image> candidateImages = Set.of(mock(Image.class));
        Set<ClouderaManagerRepo> candidateCmRepos = Set.of();
        ClouderaManagerRepo chosenClouderaManagerRepo = new ClouderaManagerRepo().withVersion(CM_VERSION);
        when(imageReaderService.getCmRepos(eq(candidateImages))).thenReturn(candidateCmRepos);
        when(cmInfoRetriever.queryCmVersion(eq(stack))).thenReturn(Optional.of(CM_VERSION));
        when(cmProductChooserService.chooseCmRepo(eq(Optional.of(CM_VERSION)), eq(candidateCmRepos))).thenReturn(Optional.of(chosenClouderaManagerRepo));

        CmRepoSyncOperationResult cmRepoSyncOperationResult = underTest.findCmRepoComponent(stack, candidateImages, null);

        assertTrue(cmRepoSyncOperationResult.getFoundClouderaManagerRepo().isPresent());
        verify(imageReaderService).getCmRepos(eq(candidateImages));
        verify(cmInfoRetriever).queryCmVersion(eq(stack));
        verify(cmProductChooserService).chooseCmRepo(eq(Optional.of(CM_VERSION)), eq(candidateCmRepos));
    }

    @Test
    void testFindParcelComponents() {
        Set<Image> candidateImages = Set.of(mock(Image.class));
        Set<ClouderaManagerProduct> candidateCmProduct = Set.of(new ClouderaManagerProduct().withName("C"));
        Set<ParcelInfo> queriedParcelInfo = Set.of(new ParcelInfo("", "", ParcelStatus.ACTIVATED));
        Set<ClouderaManagerProduct> chosenClouderaManagerProducts = Set.of(new ClouderaManagerProduct());
        when(stack.isDatalake()).thenReturn(true);
        when(imageReaderService.getParcels(candidateImages, true)).thenReturn(candidateCmProduct);
        when(cmInfoRetriever.queryActiveParcels(eq(stack))).thenReturn(queriedParcelInfo);
        when(cmProductChooserService.chooseParcelProduct(queriedParcelInfo, candidateCmProduct)).thenReturn(chosenClouderaManagerProducts);
        when(customParcelFilterService.filterCustomParcels(queriedParcelInfo, candidateCmProduct)).thenReturn(queriedParcelInfo);

        CmParcelSyncOperationResult cmParcelSyncOperationResult = underTest.findParcelComponents(stack, candidateImages, null);

        assertThat(cmParcelSyncOperationResult.getActiveParcels(), hasSize(1));
        verify(imageReaderService).getParcels(candidateImages, true);
        verify(cmInfoRetriever).queryActiveParcels(stack);
        verify(cmProductChooserService).chooseParcelProduct(queriedParcelInfo, candidateCmProduct);
        verify(customParcelFilterService).filterCustomParcels(queriedParcelInfo, candidateCmProduct);
    }

}
