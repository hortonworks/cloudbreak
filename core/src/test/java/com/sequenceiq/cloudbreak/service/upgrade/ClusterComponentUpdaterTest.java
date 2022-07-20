package com.sequenceiq.cloudbreak.service.upgrade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;

@RunWith(MockitoJUnitRunner.class)
public class ClusterComponentUpdaterTest {

    private static final long CLUSTER_ID = 1L;

    private static final String CDH = "CDH";

    private static final String FLINK = "FLINK";

    @InjectMocks
    private ClusterComponentUpdater underTest;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Test
    public void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsNoComponentToDelete() {
        Set<ClusterComponentView> clusterComponentsByBlueprint = createComponents(Set.of(CDH, FLINK));
        Set<ClusterComponentView> clusterComponentsFromDb = createComponents(Set.of(CDH, FLINK));

        when(clusterComponentConfigProvider.getComponentListByType(CLUSTER_ID, ComponentType.CDH_PRODUCT_DETAILS)).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint, new ParcelOperationStatus(Map.of(), Map.of()));

        verify(clusterComponentConfigProvider).getComponentListByType(CLUSTER_ID, ComponentType.CDH_PRODUCT_DETAILS);
        verifyNoMoreInteractions(clusterComponentConfigProvider);
    }

    @Test
    public void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsNoComponentToDeleteAndAnExtraComponentIsPresent() {
        Set<ClusterComponentView> clusterComponentsByBlueprint = createComponents(Set.of(CDH, FLINK));
        Set<ClusterComponentView> clusterComponentsFromDb = createComponents(Set.of(CDH, FLINK));

        when(clusterComponentConfigProvider.getComponentListByType(CLUSTER_ID, ComponentType.CDH_PRODUCT_DETAILS)).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint, new ParcelOperationStatus(Map.of(), Map.of()));

        verify(clusterComponentConfigProvider).getComponentListByType(CLUSTER_ID, ComponentType.CDH_PRODUCT_DETAILS);
        verifyNoMoreInteractions(clusterComponentConfigProvider);
    }

    @Test
    public void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsOneComponentToDelete() {
        Set<ClusterComponentView> clusterComponentsByBlueprint = createComponents(Set.of(CDH));
        ClusterComponentView flinkComponent = createComponent(FLINK);
        ClusterComponentView cdhComponent = createComponent(CDH);
        Set<ClusterComponentView> clusterComponentsFromDb = Set.of(flinkComponent, cdhComponent);

        when(clusterComponentConfigProvider.getComponentListByType(CLUSTER_ID, ComponentType.CDH_PRODUCT_DETAILS)).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint,
                new ParcelOperationStatus(Map.of("FLINK", "version"), Map.of()));

        verify(clusterComponentConfigProvider).getComponentListByType(CLUSTER_ID, ComponentType.CDH_PRODUCT_DETAILS);
        verify(clusterComponentConfigProvider).deleteClusterComponentViews(Set.of(flinkComponent));
    }

    private Set<ClusterComponentView> createComponents(Set<String> names) {
        return names.stream()
                .map(this::createComponent)
                .collect(Collectors.toSet());
    }

    private ClusterComponentView createComponent(String name) {
        ClusterComponentView component = new ClusterComponentView();
        component.setName(name);
        component.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        return component;
    }

    private ClusterComponentView createImageComponent() {
        ClusterComponentView component = new ClusterComponentView();
        component.setName("image");
        component.setComponentType(ComponentType.IMAGE);
        return component;
    }

}