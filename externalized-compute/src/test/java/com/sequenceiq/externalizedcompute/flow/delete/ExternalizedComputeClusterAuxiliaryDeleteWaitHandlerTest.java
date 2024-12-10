package com.sequenceiq.externalizedcompute.flow.delete;

import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterAuxiliaryDeleteWaitHandler.DELETED_STATUS;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterAuxiliaryDeleteWaitHandler.DELETE_FAILED_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClusterItem;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterAuxiliaryDeleteWaitHandlerTest {

    @Mock
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Mock
    private LiftieGrpcClient liftieGrpcClient;

    @InjectMocks
    private ExternalizedComputeClusterAuxiliaryDeleteWaitHandler externalizedComputeClusterAuxiliaryDeleteWaitHandler;

    @Test
    void doAcceptTestNoAuxiliaryClusters() {
        HandlerEvent<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getEnvironmentCrn()).thenReturn("envCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "timeLimit", 10);
        when(liftieGrpcClient.listAuxClusters("envCrn", "actorCrn")).thenReturn(List.of());

        ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse response = (ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse)
                externalizedComputeClusterAuxiliaryDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, times(1)).listAuxClusters("envCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertFalse(response.isForce());
        assertFalse(response.isPreserveCluster());
    }

    @Test
    void doAcceptTestHasAuxiliaryClusters() {
        HandlerEvent<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getEnvironmentCrn()).thenReturn("envCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "timeLimit", 10);
        ListClusterItem aux1 = mock(ListClusterItem.class);
        when(aux1.getStatus()).thenReturn("DELETING", "DELETING", DELETED_STATUS);
        ListClusterItem aux2 = ListClusterItem.newBuilder().setStatus(DELETED_STATUS).build();
        List<ListClusterItem> auxList = new ArrayList<>();
        auxList.add(aux1);
        auxList.add(aux2);
        when(liftieGrpcClient.listAuxClusters("envCrn", "actorCrn")).thenReturn(auxList);

        ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse response = (ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse)
                externalizedComputeClusterAuxiliaryDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, times(3)).listAuxClusters("envCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertFalse(response.isForce());
        assertFalse(response.isPreserveCluster());
    }

    @Test
    void doAcceptTestHasAuxiliaryClustersButTimedOut() {
        HandlerEvent<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getEnvironmentCrn()).thenReturn("envCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "sleepTime", 10);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "timeLimit", 5);
        ListClusterItem aux1 = ListClusterItem.newBuilder().setStatus("DELETING").build();
        ListClusterItem aux2 = ListClusterItem.newBuilder().setStatus(DELETED_STATUS).build();
        List<ListClusterItem> auxList = new ArrayList<>();
        auxList.add(aux1);
        auxList.add(aux2);
        when(liftieGrpcClient.listAuxClusters("envCrn", "actorCrn")).thenReturn(auxList);

        ExternalizedComputeClusterDeleteFailedEvent response = (ExternalizedComputeClusterDeleteFailedEvent)
                externalizedComputeClusterAuxiliaryDeleteWaitHandler.doAccept(handlerEvent);
        verify(liftieGrpcClient, atLeast(2)).listAuxClusters("envCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertEquals("Auxiliary cluster deletion timed out for environment: envCrn", response.getException().getMessage());
    }

    @Test
    void doAcceptTestHasAuxiliaryClustersButFailedClusterRemaining() {
        HandlerEvent<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getEnvironmentCrn()).thenReturn("envCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "timeLimit", 10);
        ListClusterItem aux1 = ListClusterItem.newBuilder().setStatus(DELETED_STATUS).setClusterName("aux1").build();
        ListClusterItem aux2 = ListClusterItem.newBuilder().setStatus(DELETE_FAILED_STATUS).setClusterName("aux2").setMessage("fail1").build();
        ListClusterItem aux3 = ListClusterItem.newBuilder().setStatus(DELETE_FAILED_STATUS).setClusterName("aux3").setMessage("fail2").build();
        List<ListClusterItem> auxList = new ArrayList<>();
        auxList.add(aux1);
        auxList.add(aux2);
        auxList.add(aux3);
        when(liftieGrpcClient.listAuxClusters("envCrn", "actorCrn")).thenReturn(auxList);

        ExternalizedComputeClusterDeleteFailedEvent response = (ExternalizedComputeClusterDeleteFailedEvent)
                externalizedComputeClusterAuxiliaryDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, atLeast(1)).listAuxClusters("envCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertEquals("""
                Auxiliary cluster deletion failed:
                aux2 -> DELETE_FAILED - Message: fail1
                aux3 -> DELETE_FAILED - Message: fail2""", response.getException().getMessage());
    }

    @Test
    void doAcceptTestHasAuxiliaryClustersButFailedClusterRemainingAndForceUsed() {
        HandlerEvent<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(2L, "actorCrn", true, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getEnvironmentCrn()).thenReturn("envCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterAuxiliaryDeleteWaitHandler, "timeLimit", 10);
        ListClusterItem aux1 = ListClusterItem.newBuilder().setStatus(DELETED_STATUS).setClusterName("aux1").build();
        ListClusterItem aux2 = ListClusterItem.newBuilder().setStatus(DELETE_FAILED_STATUS).setClusterName("aux2").setMessage("fail1").build();
        ListClusterItem aux3 = ListClusterItem.newBuilder().setStatus(DELETE_FAILED_STATUS).setClusterName("aux3").setMessage("fail2").build();
        List<ListClusterItem> auxList = new ArrayList<>();
        auxList.add(aux1);
        auxList.add(aux2);
        auxList.add(aux3);
        when(liftieGrpcClient.listAuxClusters("envCrn", "actorCrn")).thenReturn(auxList);

        ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse response = (ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse)
                externalizedComputeClusterAuxiliaryDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, atLeast(1)).listAuxClusters("envCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertTrue(response.isForce());
        assertFalse(response.isPreserveCluster());
    }
}