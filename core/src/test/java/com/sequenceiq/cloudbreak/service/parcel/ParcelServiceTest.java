package com.sequenceiq.cloudbreak.service.parcel;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class ParcelServiceTest {

    private static final long CLUSTER_ID = 2L;

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 3L;

    private static final String PARCEL_NAME = "parcel1";

    private static final Blueprint BLUEPRINT = new Blueprint();

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private ImageReaderService imageReaderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ParcelFilterService parcelFilterService;

    @Mock
    private Image image;

    @Mock
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @InjectMocks
    private ParcelService underTest;

    @Test
    void baseImageDataHubUpgradeKeepsAllExplicitProductsIncludingNewAdditionalParcel() {
        Stack stack = createStack(StackType.WORKLOAD);
        ClouderaManagerProduct cdh = createClouderaManagerProduct("CDH");
        ClouderaManagerProduct flink = createClouderaManagerProduct("FLINK");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base-image", OsType.RHEL8, "x86_64", cdh, Set.of(flink), null);

        assertEquals(Set.of(cdh, flink), underTest.getRequiredProductsForUpgrade(stack, properties));
        verifyNoInteractions(centralCDHVersionCoordinator, parcelFilterService, clouderaManagerProductTransformer);
    }

    @ParameterizedTest
    @EnumSource(value = StackType.class, names = {"DATALAKE", "WORKLOAD"})
    void prewarmedUpgradeKeepsExistingParcelSelection(StackType stackType) {
        Stack stack = createStack(stackType);
        ClouderaManagerProduct cdh = createClouderaManagerProduct("CDH");
        Set<ClouderaManagerProduct> products = Set.of(cdh, createClouderaManagerProduct("FLINK"));
        ClusterUpgradeProperties properties = mock(ClusterUpgradeProperties.class);
        ClusterUpgradeProperties.TargetImageUpgradeContext target = mock(ClusterUpgradeProperties.TargetImageUpgradeContext.class);
        when(properties.targetImage()).thenReturn(target);
        when(target.getAllProducts()).thenReturn(products);
        when(target.isBaseImage()).thenReturn(false);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(Set.of(createClusterComponent("CDH")));
        if (stackType == StackType.WORKLOAD) {
            when(parcelFilterService.filterParcelsByBlueprint(WORKSPACE_ID, STACK_ID, products, BLUEPRINT)).thenReturn(products);
        }

        assertEquals(Set.of(cdh), underTest.getRequiredProductsForUpgrade(stack, properties));
    }

    @Test
    void baseImageDatalakeUpgradeOnlySelectsRuntimeParcel() {
        Stack stack = createStack(StackType.DATALAKE);
        ClouderaManagerProduct cdh = createClouderaManagerProduct("CDH");
        ClouderaManagerProduct extra = createClouderaManagerProduct("FLINK");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.2", "base", OsType.RHEL8, "x86_64", cdh, Set.of(extra), null);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID))
                .thenReturn(Set.of(createClusterComponent("CDH"), createClusterComponent("FLINK")));

        assertEquals(Set.of(cdh), underTest.getRequiredProductsForUpgrade(stack, properties));
        verifyNoInteractions(imageReaderService, parcelFilterService);
    }

    @Test
    void datalakeParcelSelectionDoesNotDependOnCatalogAvailability() throws Exception {
        ImageService imageService = mock(ImageService.class);
        ImageReaderService realImageReader = new ImageReaderService();
        ReflectionTestUtils.setField(realImageReader, "imageService", imageService);
        ReflectionTestUtils.setField(underTest, "imageReaderService", realImageReader);
        Stack stack = createStack(StackType.DATALAKE);
        ClusterComponentView cdh = createClusterComponent("CDH");
        Set<ClusterComponentView> components = Set.of(cdh, createClusterComponent("FLINK"));
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(components);
        lenient().when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID))
                .thenThrow(new CloudbreakImageCatalogException("unavailable"));

        assertEquals(Set.of(cdh), underTest.getParcelComponentsByBlueprint(stack));
        verifyNoInteractions(imageService, parcelFilterService);
    }

    @Test
    void testRemoveUnusedComponents() throws CloudbreakException {
        Stack stack = createStack();
        Set<String> parcelNames = Set.of(PARCEL_NAME);
        Set<ClusterComponentView> clusterComponentsByBlueprint = Collections.emptySet();
        ParcelOperationStatus removalStatus = new ParcelOperationStatus();

        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(imageReaderService.getParcelNames(WORKSPACE_ID, STACK_ID)).thenReturn(parcelNames);
        when(clusterApi.removeUnusedParcels(clusterComponentsByBlueprint, parcelNames)).thenReturn(removalStatus);

        underTest.removeUnusedParcelComponents(stack, clusterComponentsByBlueprint);

        verify(imageReaderService).getParcelNames(WORKSPACE_ID, STACK_ID);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).removeUnusedParcels(clusterComponentsByBlueprint, parcelNames);
        verify(clusterComponentUpdater).removeUnusedCdhProductsFromClusterComponents(stack.getCluster().getId(), clusterComponentsByBlueprint, removalStatus);
    }

    @Test
    void testGetRequiredProductsFromImageShouldOnlyTheRequiredParcels() {
        Stack stack = createStack();
        ClouderaManagerProduct cdhParcel = createClouderaManagerProduct("CDH");
        Set<ClouderaManagerProduct> parcelsInImage = Set.of(cdhParcel, createClouderaManagerProduct("FLINK"));
        when(clouderaManagerProductTransformer.transform(image, true, true)).thenReturn(parcelsInImage);
        when(parcelFilterService.filterParcelsByBlueprint(WORKSPACE_ID, STACK_ID, parcelsInImage, BLUEPRINT)).thenReturn(Collections.singleton(cdhParcel));
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(
                Set.of(createClusterComponent("CDH"), createClusterComponent("FLINK")));

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsFromImage(stack, image);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdhParcel));
        verify(clouderaManagerProductTransformer, times(2)).transform(image, true, true);
        verify(parcelFilterService).filterParcelsByBlueprint(WORKSPACE_ID, STACK_ID, parcelsInImage, BLUEPRINT);
    }

    @Test
    void testGetRequiredProductsFromProductsShouldOnlyReturnRequiredParcelsForWorkload() {
        Stack stack = createStack();
        ClouderaManagerProduct cdhParcel = createClouderaManagerProduct("CDH");
        Set<ClouderaManagerProduct> targetProducts = Set.of(cdhParcel, createClouderaManagerProduct("FLINK"));
        when(parcelFilterService.filterParcelsByBlueprint(WORKSPACE_ID, STACK_ID, targetProducts, BLUEPRINT)).thenReturn(Collections.singleton(cdhParcel));
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(
                Set.of(createClusterComponent("CDH"), createClusterComponent("FLINK")));

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsFromProducts(stack, targetProducts);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdhParcel));
        verify(parcelFilterService).filterParcelsByBlueprint(WORKSPACE_ID, STACK_ID, targetProducts, BLUEPRINT);
    }

    @Test
    void testGetRequiredProductsFromProductsShouldExcludeParcelsWithoutClusterComponent() {
        Stack stack = createStack();
        ClouderaManagerProduct cdhParcel = createClouderaManagerProduct("CDH");
        ClouderaManagerProduct flinkParcel = createClouderaManagerProduct("FLINK");
        Set<ClouderaManagerProduct> targetProducts = Set.of(cdhParcel, flinkParcel);
        when(parcelFilterService.filterParcelsByBlueprint(WORKSPACE_ID, STACK_ID, targetProducts, BLUEPRINT)).thenReturn(targetProducts);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(Set.of(createClusterComponent("CDH")));

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsFromProducts(stack, targetProducts);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdhParcel));
    }

    @Test
    void testGetRequiredProductsFromProductsForDatalakeShouldOnlyReturnCdh() {
        Stack stack = createStack(StackType.DATALAKE);
        ClouderaManagerProduct cdhParcel = createClouderaManagerProduct("CDH");
        ClouderaManagerProduct flinkParcel = createClouderaManagerProduct("FLINK");
        Set<ClouderaManagerProduct> targetProducts = Set.of(cdhParcel, flinkParcel);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(
                Set.of(createClusterComponent("CDH"), createClusterComponent("FLINK")));

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsFromProducts(stack, targetProducts);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdhParcel));
    }

    @Test
    void testGetRequiredProductsFromProductsForDatalakeShouldReturnCdhAndSmartRouter() {
        Stack stack = createStack(StackType.DATALAKE);
        ClouderaManagerProduct cdhParcel = createClouderaManagerProduct("CDH");
        ClouderaManagerProduct smartRouterParcel = createClouderaManagerProduct(SmartRouterParcelService.COMPONENT_NAME);
        Set<ClouderaManagerProduct> targetProducts = Set.of(cdhParcel, smartRouterParcel, createClouderaManagerProduct("FLINK"));
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(datalakeClusterComponents());

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsFromProducts(stack, targetProducts);

        assertEquals(Set.of(cdhParcel, smartRouterParcel), actual);
        verifyNoInteractions(parcelFilterService);
    }

    @Test
    void testGetRequiredProductsFromImageForDatalakeShouldReturnCdhAndSmartRouter() {
        Stack stack = createStack(StackType.DATALAKE);
        ClouderaManagerProduct cdhParcel = createClouderaManagerProduct("CDH");
        ClouderaManagerProduct smartRouterParcel = createClouderaManagerProduct(SmartRouterParcelService.COMPONENT_NAME);
        when(clouderaManagerProductTransformer.transform(image, true, false))
                .thenReturn(Set.of(cdhParcel, smartRouterParcel, createClouderaManagerProduct("FLINK")));
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(datalakeClusterComponents());

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsFromImage(stack, image);

        assertEquals(Set.of(cdhParcel, smartRouterParcel), actual);
        verifyNoInteractions(parcelFilterService);
    }

    @Test
    void testGetParcelComponentsByBlueprintForDatalakeShouldReturnCdhAndSmartRouter() {
        Stack stack = createStack(StackType.DATALAKE);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID)).thenReturn(datalakeClusterComponents());

        Set<ClusterComponentView> actual = underTest.getParcelComponentsByBlueprint(stack);

        assertEquals(Set.of("CDH", SmartRouterParcelService.COMPONENT_NAME), componentNames(actual));
        verifyNoInteractions(parcelFilterService);
    }

    @Test
    void testGetParcelComponentsByBlueprintForDatalakeShouldReturnOnlyCdhWhenSmartRouterIsNotAvailable() {
        Stack stack = createStack(StackType.DATALAKE);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID))
                .thenReturn(Set.of(createClusterComponent("CDH"), createClusterComponent("FLINK")));

        Set<ClusterComponentView> actual = underTest.getParcelComponentsByBlueprint(stack);

        assertEquals(Set.of("CDH"), componentNames(actual));
    }

    @Test
    void testGetParcelComponentsByBlueprintForDatalakeShouldThrowNotFoundExceptionWhenTheCdhComponentIsMissing() {
        Stack stack = createStack(StackType.DATALAKE);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(CLUSTER_ID))
                .thenReturn(Set.of(createClusterComponent(SmartRouterParcelService.COMPONENT_NAME)));

        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.getParcelComponentsByBlueprint(stack));

        assertEquals("Runtime component not found!", exception.getMessage());
    }

    private Set<ClusterComponentView> datalakeClusterComponents() {
        return Set.of(createClusterComponent("CDH"), createClusterComponent(SmartRouterParcelService.COMPONENT_NAME), createClusterComponent("FLINK"));
    }

    private Set<String> componentNames(Set<ClusterComponentView> components) {
        return components.stream().map(ClusterComponentView::getName).collect(Collectors.toSet());
    }

    private ClouderaManagerProduct createClouderaManagerProduct(String name) {
        return new ClouderaManagerProduct().withName(name);
    }

    private ClusterComponentView createClusterComponent(String name) {
        ClusterComponentView clusterComponent = new ClusterComponentView();
        clusterComponent.setName(name);
        clusterComponent.setComponentType(cdhProductDetails());
        clusterComponent.setAttributes(new Json(createClouderaManagerProduct(name)));
        return clusterComponent;
    }

    private Stack createStack() {
        return createStack(StackType.WORKLOAD);
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setBlueprint(BLUEPRINT);
        stack.setId(STACK_ID);
        stack.setCluster(cluster);
        stack.setType(stackType);

        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        return stack;
    }
}
