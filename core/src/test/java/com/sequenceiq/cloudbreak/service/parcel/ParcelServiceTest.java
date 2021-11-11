package com.sequenceiq.cloudbreak.service.parcel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;

@ExtendWith(MockitoExtension.class)
public class ParcelServiceTest {

    private static final long CLUSTER_ID = 2L;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private ClusterApi clusterApi;

    @InjectMocks
    private ParcelService underTest;

    @Test
    void testRemoveUnusedComponents() throws CloudbreakException {
        Stack stack = createStack();
        Set<ClusterComponent> clusterComponentsByBlueprint = Collections.emptySet();
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.removeUnusedParcels(any())).thenReturn(new ParcelOperationStatus());

        underTest.removeUnusedParcelComponents(stack, clusterComponentsByBlueprint);

        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).removeUnusedParcels(clusterComponentsByBlueprint);
        verify(clusterComponentUpdater).removeUnusedCdhProductsFromClusterComponents(stack.getCluster().getId(), clusterComponentsByBlueprint,
                new ParcelOperationStatus());
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        return stack;
    }
}
