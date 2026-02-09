package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DISTRIBUTE_PARCELS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_CSD_PACKAGE_DOWNLOAD_EVENT;
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
class ClusterUpgradeParcelDistributionHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterUpgradeParcelDistributionHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private Stack stack;

    @Test
    void testDoAcceptShouldCallClusterApiToDistributeParcels() throws CloudbreakException {
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(requiredProducts));

        assertEquals(START_CLUSTER_UPGRADE_CSD_PACKAGE_DOWNLOAD_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).distributeParcels(requiredProducts);
    }

    @Test
    void testDoAcceptShouldReturnPreparationFailureEventWhenClusterApiReturnsWithAnException() throws CloudbreakException {
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        doThrow(new CloudbreakException("Failed to distribute parcels")).when(clusterApi).distributeParcels(requiredProducts);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(requiredProducts));

        assertEquals(FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).distributeParcels(requiredProducts);
    }

    private HandlerEvent<ClusterUpgradePreparationEvent> createEvent(Set<ClouderaManagerProduct> requiredProducts) {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradePreparationEvent(DISTRIBUTE_PARCELS_EVENT.name(), STACK_ID, requiredProducts, "")));
    }

}