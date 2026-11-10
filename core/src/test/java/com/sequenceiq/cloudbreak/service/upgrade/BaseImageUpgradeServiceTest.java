package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.hibernate.collection.spi.PersistentSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageRegionUpgradeImageFilter;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
class BaseImageUpgradeServiceTest {

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private ImageRegionUpgradeImageFilter imageRegionUpgradeImageFilter;

    @Mock
    private BaseImagePackageVersionsEnricher baseImagePackageVersionsEnricher;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private UpgradePermissionProvider upgradePermissionProvider;

    @InjectMocks
    private BaseImageUpgradeService underTest;

    @Test
    void selectsLatestBaseImageEvenWhenCurrentPrewarmedImageWasBuiltLater() throws Exception {
        StackDto stack = stackDto();
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stack.getWorkspaceId()).thenReturn(2L);
        when(stackView.getCloudPlatform()).thenReturn("AWS");
        when(stackView.getPlatformVariant()).thenReturn("AWS");
        when(stackView.getRegion()).thenReturn("eu-west-1");
        Image current = Image.builder().withImageCatalogName("catalog").withOsType("redhat8").withArchitecture("x86_64")
                .withPackageVersions(Map.of(ImagePackageVersion.STACK.getKey(), "7.3.1", ImagePackageVersion.CM.getKey(), "7.13.1",
                        ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), "12345")).withCreated(2L).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(3L);
        when(baseImagePackageVersionsEnricher.enrich(current, 3L)).thenReturn(current);
        when(platformStringTransformer.getPlatformStringForImageCatalog("AWS", "AWS")).thenReturn(imageCatalogPlatform("AWS"));
        Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> regionFilter = image -> true;
        when(imageRegionUpgradeImageFilter.filterByRegion(imageCatalogPlatform("AWS"), "AWS", "eu-west-1")).thenReturn(regionFilter);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image base = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid("base").withCreated(1L).build();
        ArgumentCaptor<ImageFilter> filter = ArgumentCaptor.forClass(ImageFilter.class);
        when(imageCatalogService.getLatestBaseImage(filter.capture())).thenReturn(StatedImage.statedImage(base, "catalog-url", "catalog"));
        UpgradeComponents components = new UpgradeComponents(null, null);

        ImageChangeDto result = underTest.createImageChangeDto(stack, components);

        assertThat(result.getImageId()).isEqualTo("base");
        assertThat(result.getImageCatalogUrl()).isEqualTo("catalog-url");
        assertThat(result.getComponents()).isSameAs(components);
        verify(upgradePermissionProvider).validateRequestedComponentVersions(current.getPackageVersions(), Set.of(), components);
        assertThat(filter.getValue().isBaseImageEnabled()).isTrue();
        assertThat(filter.getValue().isDefaultOnly()).isFalse();
        assertThat(filter.getValue().getClusterVersion()).isNull();
        assertThat(filter.getValue().getPlatforms()).isEqualTo(Set.of(imageCatalogPlatform("AWS")));
        assertThat(filter.getValue().getOperatingSystems()).isEqualTo(Set.of("redhat8"));
        assertThat(filter.getValue().getArchitecture()).isEqualTo(Architecture.X86_64);
        assertThat(filter.getValue().getAdditionalPredicate()).isSameAs(regionFilter);
    }

    @Test
    void usesGivenBaseImageFromCurrentCatalog() throws Exception {
        StackDto stack = stackDto();
        when(stack.getWorkspaceId()).thenReturn(2L);
        Image current = Image.builder().withImageCatalogName("catalog").withPackageVersions(Map.of()).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        Image enriched = Image.builder().withPackageVersions(Map.of(ImagePackageVersion.CM.getKey(), "7.13.2")).build();
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(3L);
        when(baseImagePackageVersionsEnricher.enrich(current, 3L)).thenReturn(enriched);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image base = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder().withUuid("original").build();
        when(imageCatalogService.getImageByCatalogName(2L, "original", "catalog")).thenReturn(StatedImage.statedImage(base, "catalog-url", "catalog"));
        UpgradeComponents components = new UpgradeComponents(null, null);

        ImageChangeDto result = underTest.createImageChangeDto(stack, components, "original");

        assertThat(result.getImageId()).isEqualTo("original");
        assertThat(result.getImageCatalogName()).isEqualTo("catalog");
        assertThat(result.getImageCatalogUrl()).isEqualTo("catalog-url");
        assertThat(result.getComponents()).isSameAs(components);
        verify(upgradePermissionProvider).validateRequestedComponentVersions(enriched.getPackageVersions(), Set.of(), components);
        verify(imageCatalogService, never()).getLatestBaseImage(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void resolvesBaseImageWithDetachedClusterComponents(boolean reinitiation) throws Exception {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform("AWS");
        stack.setPlatformVariant("AWS");
        stack.setRegion("eu-west-1");
        Workspace workspace = new Workspace();
        workspace.setId(2L);
        stack.setWorkspace(workspace);
        Cluster cluster = new Cluster();
        cluster.setId(3L);
        PersistentSet<ClusterComponent> clusterComponents = new PersistentSet<>(null);
        cluster.setComponents(clusterComponents);
        stack.setCluster(cluster);
        Image current = Image.builder().withImageCatalogName("catalog").withOsType("redhat8").withArchitecture("x86_64")
                .withPackageVersions(Map.of(ImagePackageVersion.STACK.getKey(), "7.3.1")).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        when(baseImagePackageVersionsEnricher.enrich(current, 3L)).thenReturn(current);
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName("CDH").withVersion("7.3.1-1.cdh7.3.1.p1200.84201983");
        when(clusterComponentConfigProvider.getCdhProduct(3L)).thenReturn(Optional.of(cdh));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image base = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder().withUuid("base").build();
        StatedImage targetImage = StatedImage.statedImage(base, "catalog-url", "catalog");
        if (reinitiation) {
            when(imageCatalogService.getImageByCatalogName(2L, "base", "catalog")).thenReturn(targetImage);
        } else {
            when(platformStringTransformer.getPlatformStringForImageCatalog("AWS", "AWS")).thenReturn(imageCatalogPlatform("AWS"));
            when(imageRegionUpgradeImageFilter.filterByRegion(imageCatalogPlatform("AWS"), "AWS", "eu-west-1")).thenReturn(image -> true);
            when(imageCatalogService.getLatestBaseImage(any())).thenReturn(targetImage);
        }
        UpgradeComponents components = new UpgradeComponents(null, null);

        ImageChangeDto result = reinitiation ? underTest.createImageChangeDto(stack, components, "base")
                : underTest.createImageChangeDto(stack, components);

        assertThat(result.getImageId()).isEqualTo("base");
        assertThat(result.getComponents()).isSameAs(components);
        assertThat(clusterComponents.wasInitialized()).isFalse();
        verify(clusterComponentConfigProvider).getCdhProduct(3L);
        verify(upgradePermissionProvider).validateRequestedComponentVersions(current.getPackageVersions(), Set.of(cdh), components);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsDowngradeBeforeLookingUpLatestOrPinnedImage(boolean reinitiation) throws Exception {
        StackDto stack = stackDto();
        Image current = Image.builder().withImageId("current").withPackageVersions(Map.of()).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(3L);
        when(baseImagePackageVersionsEnricher.enrich(current, 3L)).thenReturn(current);
        UpgradeComponents components = new UpgradeComponents(null, null);
        BadRequestException failure = new BadRequestException("Cannot downgrade Cloudera Manager");
        doThrow(failure).when(upgradePermissionProvider).validateRequestedComponentVersions(current.getPackageVersions(), Set.of(), components);

        assertThatThrownBy(() -> {
            if (reinitiation) {
                underTest.createImageChangeDto(stack, components, "original-base-image");
            } else {
                underTest.createImageChangeDto(stack, components);
            }
        }).isSameAs(failure);

        verifyNoInteractions(imageCatalogService, platformStringTransformer, imageRegionUpgradeImageFilter);
    }

    private StackDto stackDto() {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        Cluster cluster = new Cluster();
        cluster.setId(3L);
        when(stack.getCluster()).thenReturn(cluster);
        return stack;
    }

}
