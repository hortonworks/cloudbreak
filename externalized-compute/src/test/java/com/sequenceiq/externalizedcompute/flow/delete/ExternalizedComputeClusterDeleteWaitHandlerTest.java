package com.sequenceiq.externalizedcompute.flow.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DescribeClusterResponse;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterDeleteWaitHandlerTest {

    @Mock
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Mock
    private LiftieGrpcClient liftieGrpcClient;

    @InjectMocks
    private ExternalizedComputeClusterDeleteWaitHandler externalizedComputeClusterDeleteWaitHandler;

    @Test
    void doAcceptTestWithDeleted() {
        HandlerEvent<ExternalizedComputeClusterDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getLiftieName()).thenReturn("liftieName");
        when(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster)).thenReturn("liftieCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "timeLimit", 10);
        when(liftieGrpcClient.describeCluster(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster), "actorCrn"))
                .thenReturn(DescribeClusterResponse.newBuilder().setStatus("DELETED").build());

        ExternalizedComputeClusterDeleteWaitSuccessResponse response = (ExternalizedComputeClusterDeleteWaitSuccessResponse)
                externalizedComputeClusterDeleteWaitHandler.doAccept(handlerEvent);

        assertEquals(2L, response.getResourceId());
    }

    @Test
    void doAcceptTestWithDeleteInProgressThenDeleted() {
        HandlerEvent<ExternalizedComputeClusterDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getLiftieName()).thenReturn("liftieName");
        when(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster)).thenReturn("liftieCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "timeLimit", 10);
        when(liftieGrpcClient.describeCluster(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster), "actorCrn"))
                .thenReturn(DescribeClusterResponse.newBuilder().setStatus("DELETE_IN_PROGRESS").build())
                .thenReturn(DescribeClusterResponse.newBuilder().setStatus("DELETED").build());

        ExternalizedComputeClusterDeleteWaitSuccessResponse response = (ExternalizedComputeClusterDeleteWaitSuccessResponse)
                externalizedComputeClusterDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, times(2)).describeCluster("liftieCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
    }

    @Test
    void doAcceptTestWithDeleteInProgressThenDeleteFailed() {
        HandlerEvent<ExternalizedComputeClusterDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterDeleteWaitRequest(2L, "actorCrn", false, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getLiftieName()).thenReturn("liftieName");
        when(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster)).thenReturn("liftieCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "timeLimit", 10);
        when(liftieGrpcClient.describeCluster(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster), "actorCrn"))
                .thenReturn(DescribeClusterResponse.newBuilder().setStatus("DELETE_IN_PROGRESS").build())
                .thenReturn(DescribeClusterResponse.newBuilder().setMessage("failMessage").setStatus("DELETE_FAILED").build());

        ExternalizedComputeClusterDeleteFailedEvent response = (ExternalizedComputeClusterDeleteFailedEvent)
                externalizedComputeClusterDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, times(2)).describeCluster("liftieCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertEquals("Cluster deletion failed. Status: DELETE_FAILED. Message: failMessage", response.getException().getMessage());
    }

    @Test
    void doAcceptTestWithDeleteInProgressThenDeleteFailedButForceDeleted() {
        HandlerEvent<ExternalizedComputeClusterDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterDeleteWaitRequest(2L, "actorCrn", true, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getLiftieName()).thenReturn("liftieName");
        when(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster)).thenReturn("liftieCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "timeLimit", 2);
        when(liftieGrpcClient.describeCluster(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster), "actorCrn"))
                .thenReturn(DescribeClusterResponse.newBuilder().setStatus("DELETE_IN_PROGRESS").build())
                .thenReturn(DescribeClusterResponse.newBuilder().setMessage("failMessage").setStatus("DELETE_FAILED").build());

        ExternalizedComputeClusterDeleteWaitSuccessResponse response = (ExternalizedComputeClusterDeleteWaitSuccessResponse)
                externalizedComputeClusterDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, times(2)).describeCluster("liftieCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertTrue(response.isForce());
    }

    @Test
    void doAcceptTestWithDeleteInProgressThenDeleteTimedOutButForceDeleted() {
        HandlerEvent<ExternalizedComputeClusterDeleteWaitRequest> handlerEvent = new HandlerEvent<>(
                new Event<>(new ExternalizedComputeClusterDeleteWaitRequest(2L, "actorCrn", true, false)));
        ExternalizedComputeCluster externalizedComputeCluster = mock(ExternalizedComputeCluster.class);
        when(externalizedComputeCluster.getLiftieName()).thenReturn("liftieName");
        when(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster)).thenReturn("liftieCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(2L)).thenReturn(externalizedComputeCluster);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "sleepTime", 50);
        ReflectionTestUtils.setField(externalizedComputeClusterDeleteWaitHandler, "timeLimit", 2);
        when(liftieGrpcClient.describeCluster(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster), "actorCrn"))
                .thenReturn(DescribeClusterResponse.newBuilder().setStatus("DELETE_IN_PROGRESS").build());

        ExternalizedComputeClusterDeleteWaitSuccessResponse response = (ExternalizedComputeClusterDeleteWaitSuccessResponse)
                externalizedComputeClusterDeleteWaitHandler.doAccept(handlerEvent);

        verify(liftieGrpcClient, atLeastOnce()).describeCluster("liftieCrn", "actorCrn");
        assertEquals(2L, response.getResourceId());
        assertTrue(response.isForce());
    }
}