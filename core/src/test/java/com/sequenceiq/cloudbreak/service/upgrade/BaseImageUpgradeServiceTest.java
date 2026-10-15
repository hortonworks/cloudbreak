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
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageRegionUpgradeImageFilter;
import com.sequenceiq.cloudbreak.view.StackView;
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
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private UpgradePermissionProvider upgradePermissionProvider;

    @InjectMocks
    private BaseImageUpgradeService underTest;

    @Test
    void selectsLatestBaseImageEvenWhenCurrentPrewarmedImageWasBuiltLater() throws Exception {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStack()).thenReturn(stackView);
        when(stack.getWorkspaceId()).thenReturn(2L);
        when(stackView.getCloudPlatform()).thenReturn("AWS");
        when(stackView.getPlatformVariant()).thenReturn("AWS");
        when(stackView.getRegion()).thenReturn("eu-west-1");
        Image current = Image.builder().withImageCatalogName("catalog").withOsType("redhat8").withArchitecture("x86_64")
                .withPackageVersions(Map.of(ImagePackageVersion.STACK.getKey(), "7.3.1")).withCreated(2L).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        when(baseImagePackageVersionsEnricher.enrich(current, stack)).thenReturn(current);
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
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getWorkspaceId()).thenReturn(2L);
        Image current = Image.builder().withImageCatalogName("catalog").withPackageVersions(Map.of()).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        Image enriched = Image.builder().withPackageVersions(Map.of(ImagePackageVersion.CM.getKey(), "7.13.2")).build();
        when(baseImagePackageVersionsEnricher.enrich(current, stack)).thenReturn(enriched);
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
    void rejectsDowngradeBeforeLookingUpLatestOrPinnedImage(boolean reinitiation) throws Exception {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(1L);
        Image current = Image.builder().withImageId("current").withPackageVersions(Map.of()).build();
        when(componentConfigProviderService.getImage(1L)).thenReturn(current);
        when(baseImagePackageVersionsEnricher.enrich(current, stack)).thenReturn(current);
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

}
