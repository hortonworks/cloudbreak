package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_CM_PACKAGE_DOWNLOAD_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesFactory;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeParcelSettingsPreparationHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "image-id";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    @InjectMocks
    private ClusterUpgradeParcelSettingsPreparationHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ParcelService parcelService;

    @Mock
    private ClusterUpgradePropertiesResolver resolver;

    @Mock
    private OsChangeService osChangeService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private StackDto stackDto;

    @Test
    void testDoAcceptShouldCallClusterApiToUpdateParcelSettings() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException,
            CloudbreakException {
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL);
        Set<ClouderaManagerProduct> requiredProducts = Set.of(new ClouderaManagerProduct(), new ClouderaManagerProduct());
        Set<ClouderaManagerProduct> updatedProducts = Set.of(new ClouderaManagerProduct());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image", OsType.RHEL9, Architecture.X86_64.getName(), null, requiredProducts, null);
        when(resolver.resolve(any(ClusterUpgradeParcelSettingsPreparationEvent.class))).thenReturn(properties);
        when(parcelService.getRequiredProductsFromProducts(stackDto, properties.getAllTargetProducts())).thenReturn(requiredProducts);
        when(osChangeService.updatePreWarmParcelUrlInCaseOfOsChange(anySet(), eq(OsType.RHEL8), eq(OsType.RHEL9), eq(Architecture.X86_64.getName())))
                .thenReturn(updatedProducts);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(imageChangeDto));

        assertEquals(START_CLUSTER_UPGRADE_CM_PACKAGE_DOWNLOAD_EVENT.name(), nextFlowStepSelector.selector());
        ClusterUpgradePreparationEvent result = (ClusterUpgradePreparationEvent) nextFlowStepSelector;
        assertSame(properties, result.getClusterUpgradeProperties());
        assertEquals(updatedProducts, result.getClouderaManagerProducts());
        verify(stackDtoService).getById(STACK_ID);
        verify(parcelService).getRequiredProductsFromProducts(stackDto, properties.getAllTargetProducts());
        verify(clusterApiConnectors).getConnector(stackDto);
        verify(clusterApi).updateParcelSettings(updatedProducts);
    }

    @Test
    void testDoAcceptShouldReturnPreparationFailureEventWhenClusterApiReturnsWithAnException()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException,
            CloudbreakException {
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL);
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image", OsType.RHEL9, Architecture.X86_64.getName(), null, requiredProducts, null);
        when(resolver.resolve(any(ClusterUpgradeParcelSettingsPreparationEvent.class))).thenReturn(properties);
        when(parcelService.getRequiredProductsFromProducts(stackDto, properties.getAllTargetProducts())).thenReturn(requiredProducts);
        when(osChangeService.updatePreWarmParcelUrlInCaseOfOsChange(anySet(), eq(OsType.RHEL8), eq(OsType.RHEL9), eq(Architecture.X86_64.getName())))
                .thenReturn(requiredProducts);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        doThrow(new CloudbreakException("Failed to update parcel settings")).when(clusterApi).updateParcelSettings(requiredProducts);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(imageChangeDto));

        assertEquals(FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(parcelService).getRequiredProductsFromProducts(stackDto, properties.getAllTargetProducts());
        verify(clusterApiConnectors).getConnector(stackDto);
        verify(clusterApi).updateParcelSettings(requiredProducts);
    }

    @Test
    void testOsChangePreservesOriginalTargetParcel() throws Exception {
        ClouderaManagerProduct product = new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2-build")
                .withParcel("target-os-url").withParcelFileUrl("parcel-file").withCsd(List.of("csd-url"));
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image", OsType.RHEL9, "x86_64", product, Set.of(), null);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(resolver.resolve(any(ClusterUpgradeParcelSettingsPreparationEvent.class))).thenReturn(properties);
        when(parcelService.getRequiredProductsFromProducts(stackDto, properties.getAllTargetProducts())).thenReturn(Set.of(product));
        when(osChangeService.updatePreWarmParcelUrlInCaseOfOsChange(anySet(), eq(OsType.RHEL8), eq(OsType.RHEL9), eq("x86_64")))
                .thenAnswer(invocation -> {
                    Set<ClouderaManagerProduct> copies = invocation.getArgument(0);
                    copies.iterator().next().withParcel("current-os-url");
                    return copies;
                });
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        ClusterUpgradePreparationEvent result = (ClusterUpgradePreparationEvent) underTest.doAccept(
                createEvent(new ImageChangeDto(STACK_ID, IMAGE_ID)));

        ClouderaManagerProduct prepared = result.getClouderaManagerProducts().iterator().next();
        assertNotSame(product, prepared);
        assertEquals("current-os-url", prepared.getParcel());
        assertEquals("target-os-url", product.getParcel());
        assertEquals(product.getName(), prepared.getName());
        assertEquals(product.getVersion(), prepared.getVersion());
        assertEquals(product.getParcelFileUrl(), prepared.getParcelFileUrl());
        assertEquals(product.getCsd(), prepared.getCsd());
        assertSame(properties, result.getClusterUpgradeProperties());
    }

    @Test
    void baseImageProductsSurviveRestartAndRepeatedOsAdjustmentWithoutCatalogLookup() throws Exception {
        ClouderaManagerProduct suppliedProduct = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20")
                .withParcel("https://repo/flink/redhat9/yum/").withParcelFileUrl("https://repo/flink/FLINK.parcel")
                .withCsd(List.of("https://repo/flink/FLINK.jar"));
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image-version", OsType.RHEL9, "x86_64", null, Set.of(suppliedProduct), null);
        ClusterUpgradeParcelSettingsPreparationEvent request = JsonUtil.readValue(JsonUtil.writeValueAsString(
                new ClusterUpgradeParcelSettingsPreparationEvent(STACK_ID, new ImageChangeDto(STACK_ID, properties.targetImageId()),
                        OsType.RHEL8, properties)), ClusterUpgradeParcelSettingsPreparationEvent.class);
        ClusterUpgradePropertiesFactory factory = mock(ClusterUpgradePropertiesFactory.class);
        ReflectionTestUtils.setField(underTest, "clusterUpgradePropertiesResolver", new ClusterUpgradePropertiesResolver(factory));
        ReflectionTestUtils.setField(underTest, "osChangeService", new OsChangeService());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        Set<ClouderaManagerProduct> restoredProducts = request.getClusterUpgradeProperties().getAllTargetProducts();
        when(parcelService.getRequiredProductsFromProducts(stackDto, restoredProducts)).thenReturn(restoredProducts);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        for (int attempt = 0; attempt < 2; attempt++) {
            ClusterUpgradePreparationEvent result = (ClusterUpgradePreparationEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(request)));
            ClouderaManagerProduct prepared = result.getClouderaManagerProducts().iterator().next();
            assertThat(prepared.getParcel()).isEqualTo("https://repo/flink/redhat8/yum/");
            assertThat(prepared.getCsd()).containsExactly("https://repo/flink/FLINK.jar");
            assertThat(restoredProducts.iterator().next().getParcel()).isEqualTo("https://repo/flink/redhat9/yum/");
            assertThat(result.getClusterUpgradeProperties().targetImage().stackDetails()).isNull();
        }
        verifyNoInteractions(factory);
    }

    private HandlerEvent<ClusterUpgradeParcelSettingsPreparationEvent> createEvent(ImageChangeDto imageChangeDto) {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradeParcelSettingsPreparationEvent(STACK_ID, imageChangeDto, OsType.RHEL8, null)));
    }

}
