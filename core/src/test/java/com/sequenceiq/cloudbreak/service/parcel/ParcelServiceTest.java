package com.sequenceiq.cloudbreak.service.parcel;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

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

    private ClouderaManagerProduct createClouderaManagerProduct(String name) {
        return new ClouderaManagerProduct().withName(name);
    }

    private ClusterComponentView createClusterComponent(String name) {
        ClusterComponentView clusterComponent = new ClusterComponentView();
        clusterComponent.setName(name);
        clusterComponent.setComponentType(cdhProductDetails());
        return clusterComponent;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setBlueprint(BLUEPRINT);
        stack.setId(STACK_ID);
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);

        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        return stack;
    }
}
