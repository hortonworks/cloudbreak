package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ClusterComponentUpdaterTest {

    private static final long CLUSTER_ID = 1L;

    private static final String CDH = "CDH";

    private static final String FLINK = "FLINK";

    @InjectMocks
    private ClusterComponentUpdater underTest;

    @Mock
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private StackService stackService;

    @Test
    void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsNoComponentToDelete() {
        Set<ClusterComponentView> clusterComponentsByBlueprint = createComponents(Set.of(CDH, FLINK));
        Set<ClusterComponentView> clusterComponentsFromDb = createComponents(Set.of(CDH, FLINK));

        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(anyLong())).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint, new ParcelOperationStatus(Map.of(), Map.of()));

        verify(centralCDHVersionCoordinator).getClouderaManagerProductsFromComponents(anyLong());
        verifyNoMoreInteractions(clusterComponentConfigProvider);
    }

    @Test
    void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsNoComponentToDeleteAndAnExtraComponentIsPresent() {
        Set<ClusterComponentView> clusterComponentsByBlueprint = createComponents(Set.of(CDH, FLINK));
        Set<ClusterComponentView> clusterComponentsFromDb = createComponents(Set.of(CDH, FLINK));

        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(anyLong())).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint, new ParcelOperationStatus(Map.of(), Map.of()));

        verify(centralCDHVersionCoordinator).getClouderaManagerProductsFromComponents(anyLong());
        verifyNoMoreInteractions(clusterComponentConfigProvider);
    }

    @Test
    void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsOneComponentToDelete() {
        Set<ClusterComponentView> clusterComponentsByBlueprint = createComponents(Set.of(CDH));
        ClusterComponentView flinkComponent = createComponent(FLINK);
        ClusterComponentView cdhComponent = createComponent(CDH);
        Set<ClusterComponentView> clusterComponentsFromDb = Set.of(flinkComponent, cdhComponent);

        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(anyLong())).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint,
                new ParcelOperationStatus(Map.of("FLINK", "version"), Map.of()));

        verify(centralCDHVersionCoordinator).getClouderaManagerProductsFromComponents(anyLong());
        verify(clusterComponentConfigProvider).deleteClusterComponentViews(Set.of(flinkComponent));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addsMissingParcelOnlyWhenRequestedAndUpdatesExistingParcelWithoutDuplicatesOnRetry(boolean addMissingParcels) {
        Stack stack = new Stack();
        stack.setId(2L);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        Json oldCdh = new Json(new ClouderaManagerProduct().withName(CDH).withVersion("old"));
        Json newCdh = new Json(new ClouderaManagerProduct().withName(CDH).withVersion("new"));
        Json flink = new Json(new ClouderaManagerProduct().withName(FLINK).withVersion("1.20").withParcel("https://repo/flink"));
        ClusterComponent existingCdh = new ClusterComponent(cdhProductDetails(), CDH, oldCdh, cluster);
        Set<ClusterComponent> storedComponents = new HashSet<>(Set.of(existingCdh));
        Component targetCdh = new Component(cdhProductDetails(), cdhProductDetails().name(), newCdh, stack);
        Component targetFlink = new Component(cdhProductDetails(), FLINK, flink, stack);
        when(clusterComponentConfigProvider.getComponentsByClusterId(CLUSTER_ID)).thenReturn(storedComponents);
        if (addMissingParcels) {
            when(centralCDHVersionCoordinator.isCdhProductDetails(targetFlink)).thenReturn(true);
        }

        underTest.updateClusterComponentsByStackId(stack, Set.of(targetCdh, targetFlink), false, addMissingParcels);
        underTest.updateClusterComponentsByStackId(stack, Set.of(targetCdh, targetFlink), false, addMissingParcels);

        assertThat(storedComponents).extracting(ClusterComponent::getName)
                .containsExactlyInAnyOrderElementsOf(addMissingParcels ? Set.of(CDH, FLINK) : Set.of(CDH));
        assertThat(existingCdh.getAttributes()).isEqualTo(newCdh);
        if (addMissingParcels) {
            assertThat(storedComponents.stream().filter(component -> FLINK.equals(component.getName())).findFirst().orElseThrow().getAttributes())
                    .isEqualTo(flink);
        }
    }

    private Set<ClusterComponentView> createComponents(Set<String> names) {
        return names.stream()
                .map(this::createComponent)
                .collect(Collectors.toSet());
    }

    private ClusterComponentView createComponent(String name) {
        ClusterComponentView component = new ClusterComponentView();
        component.setName(name);
        component.setComponentType(cdhProductDetails());
        return component;
    }

    private ClusterComponentView createImageComponent() {
        ClusterComponentView component = new ClusterComponentView();
        component.setName("image");
        component.setComponentType(ComponentType.IMAGE);
        return component;
    }

}