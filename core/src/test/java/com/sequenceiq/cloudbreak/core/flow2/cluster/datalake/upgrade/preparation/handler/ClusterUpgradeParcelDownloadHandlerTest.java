package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_PARCELS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeParcelDownloadHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterUpgradeParcelDownloadHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private Stack stack;

    @Test
    void testDoAcceptShouldCallClusterApiToDownloadParcels() throws CloudbreakException {
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(requiredProducts));

        assertEquals(START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).downloadParcels(requiredProducts);
    }

    @Test
    void testDoAcceptShouldReturnPreparationFailureEventWhenClusterApiReturnsWithAnException() throws CloudbreakException {
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        doThrow(new CloudbreakException("Failed to download parcels")).when(clusterApi).downloadParcels(requiredProducts);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(requiredProducts));

        assertEquals(FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).downloadParcels(requiredProducts);
    }

    private HandlerEvent<ClusterUpgradePreparationEvent> createEvent(Set<ClouderaManagerProduct> requiredProducts) {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradePreparationEvent(DOWNLOAD_PARCELS_EVENT.name(), STACK_ID, requiredProducts)));
    }

}