package com.sequenceiq.cloudbreak.service.parcel;

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
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;

@ExtendWith(MockitoExtension.class)
public class ParcelServiceTest {

    private static final long CLUSTER_ID = 2L;

    private static final long STACK_ID = 1L;

    private static final String PARCEL_NAME = "parcel1";

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

    @InjectMocks
    private ParcelService underTest;

    @Test
    void testRemoveUnusedComponents() throws CloudbreakException, CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack();
        Set<String> parcelNames = Set.of(PARCEL_NAME);
        Set<ClusterComponent> clusterComponentsByBlueprint = Collections.emptySet();
        ParcelOperationStatus removalStatus = new ParcelOperationStatus();

        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(imageReaderService.getParcelNames(STACK_ID, stack.isDatalake())).thenReturn(parcelNames);
        when(clusterApi.removeUnusedParcels(clusterComponentsByBlueprint, parcelNames)).thenReturn(removalStatus);

        underTest.removeUnusedParcelComponents(stack, clusterComponentsByBlueprint);

        verify(imageReaderService).getParcelNames(STACK_ID, stack.isDatalake());
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).removeUnusedParcels(clusterComponentsByBlueprint, parcelNames);
        verify(clusterComponentUpdater).removeUnusedCdhProductsFromClusterComponents(stack.getCluster().getId(), clusterComponentsByBlueprint, removalStatus);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setId(STACK_ID);
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);
        return stack;
    }
}
