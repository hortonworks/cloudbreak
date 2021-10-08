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
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

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
        Set<ClusterComponent> clusterComponentsByBlueprint = createComponents(Set.of(CDH, FLINK));
        Set<ClusterComponent> clusterComponentsFromDb = createComponents(Set.of(CDH, FLINK));

        when(clusterComponentConfigProvider.getComponentsByClusterId(CLUSTER_ID)).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint, new ParcelOperationStatus(Map.of(), Map.of()));

        verify(clusterComponentConfigProvider).getComponentsByClusterId(CLUSTER_ID);
        verifyNoMoreInteractions(clusterComponentConfigProvider);
    }

    @Test
    public void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsNoComponentToDeleteAndAnExtraComponentIsPresent() {
        Set<ClusterComponent> clusterComponentsByBlueprint = createComponents(Set.of(CDH, FLINK));
        Set<ClusterComponent> clusterComponentsFromDb = createComponents(Set.of(CDH, FLINK));
        clusterComponentsFromDb.add(createImageComponent());

        when(clusterComponentConfigProvider.getComponentsByClusterId(CLUSTER_ID)).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint, new ParcelOperationStatus(Map.of(), Map.of()));

        verify(clusterComponentConfigProvider).getComponentsByClusterId(CLUSTER_ID);
        verifyNoMoreInteractions(clusterComponentConfigProvider);
    }

    @Test
    public void testRemoveUnusedCdhProductsFromClusterComponentsWhenThereIsOneComponentToDelete() {
        Set<ClusterComponent> clusterComponentsByBlueprint = createComponents(Set.of(CDH));
        ClusterComponent flinkComponent = createComponent(FLINK);
        ClusterComponent cdhComponent = createComponent(CDH);
        Set<ClusterComponent> clusterComponentsFromDb = Set.of(flinkComponent, cdhComponent);

        when(clusterComponentConfigProvider.getComponentsByClusterId(CLUSTER_ID)).thenReturn(clusterComponentsFromDb);

        underTest.removeUnusedCdhProductsFromClusterComponents(CLUSTER_ID, clusterComponentsByBlueprint,
                new ParcelOperationStatus(Map.of("FLINK", "version"), Map.of()));

        verify(clusterComponentConfigProvider).getComponentsByClusterId(CLUSTER_ID);
        verify(clusterComponentConfigProvider).deleteClusterComponents(Set.of(flinkComponent));
    }

    private Set<ClusterComponent> createComponents(Set<String> names) {
        return names.stream()
                .map(this::createComponent)
                .collect(Collectors.toSet());
    }

    private ClusterComponent createComponent(String name) {
        ClusterComponent component = new ClusterComponent();
        component.setName(name);
        component.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        return component;
    }

    private ClusterComponent createImageComponent() {
        ClusterComponent component = new ClusterComponent();
        component.setName("image");
        component.setComponentType(ComponentType.IMAGE);
        return component;
    }

}