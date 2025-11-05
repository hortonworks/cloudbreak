package com.sequenceiq.datalake.flow.datalake.scale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent.DatalakeHorizontalScaleFlowEventBuilder;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DatalakeHorizontalScaleServicesRollingRestartWaitHandlerTest {

    private long sdxId = 1L;

    private String userId = "userId";

    private String operationId = "operationId";

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private SdxService sdxService;

    @Mock
    private DistroxService distroxService;

    @InjectMocks
    private DatalakeHorizontalScaleServicesRollingRestartWaitHandler underTest;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", 10);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", 30);
    }

    @Test
    public void testSuccessfulPolling() {
        DatalakeHorizontalScaleFlowEvent datalakeHorizontalScaleFlowEvent =
                new DatalakeHorizontalScaleFlowEventBuilder()
                        .setResourceId(sdxId)
                        .setUserId(userId)
                        .build();

        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxService.getById(sdxId)).thenReturn(sdxCluster);

        DatalakeHorizontalScaleFlowEvent result = (DatalakeHorizontalScaleFlowEvent) underTest.doAccept(new HandlerEvent<>(
                new Event<>(datalakeHorizontalScaleFlowEvent)));

        verify(cloudbreakPoller).pollUpdateUntilAvailable(eq("Datalake horizontal scaling"), eq(sdxCluster), any(PollingConfig.class));
        assertEquals(sdxId, result.getResourceId());
        assertEquals(userId, result.getUserId());
    }

    @Test
    public void testUserBreakExceptionWhilePolling() {
        DatalakeHorizontalScaleFlowEvent datalakeHorizontalScaleFlowEvent =
                new DatalakeHorizontalScaleFlowEventBuilder()
                        .setResourceId(sdxId)
                        .setUserId(userId)
                        .build();

        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxService.getById(sdxId)).thenReturn(sdxCluster);
        doThrow(new UserBreakException()).
                when(cloudbreakPoller).pollUpdateUntilAvailable(eq("Datalake horizontal scaling"),
                        eq(sdxCluster), any(PollingConfig.class));

        DatalakeHorizontalScaleFlowEvent result =
                (DatalakeHorizontalScaleFlowEvent) underTest.doAccept(
                        new HandlerEvent<>(new Event<>(datalakeHorizontalScaleFlowEvent)));

        assertEquals(UserBreakException.class, result.getException().getClass());
        assertEquals(sdxId, result.getResourceId());
        assertEquals(userId, result.getUserId());
    }

    @Test
    public void testPollerStoppedExceptionWhilePolling() {
        DatalakeHorizontalScaleFlowEvent datalakeHorizontalScaleFlowEvent =
                new DatalakeHorizontalScaleFlowEventBuilder()
                        .setResourceId(sdxId)
                        .setUserId(userId)
                        .build();

        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxService.getById(sdxId)).thenReturn(sdxCluster);
        doThrow(new PollerStoppedException()).
                when(cloudbreakPoller).pollUpdateUntilAvailable(eq("Datalake horizontal scaling"), eq(sdxCluster),
                        any(PollingConfig.class));

        DatalakeHorizontalScaleFlowEvent result =
                (DatalakeHorizontalScaleFlowEvent) underTest.doAccept(
                        new HandlerEvent<>(new Event<>(datalakeHorizontalScaleFlowEvent)));

        assertEquals("Services rolling restart timed out after 30 minutes", result.getException().getMessage());
        assertEquals(PollerStoppedException.class, result.getException().getClass());
        assertEquals(sdxId, result.getResourceId());
        assertEquals(userId, result.getUserId());
    }
}
