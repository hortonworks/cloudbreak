package com.sequenceiq.datalake.flow.enableselinux.handler;

import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_DATALAKE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeEnableSeLinuxHandlerTest {

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private DatalakeEnableSeLinuxHandler underTest;

    private DatalakeEnableSeLinuxHandlerEvent handlerEvent;

    @Mock
    private SdxCluster sdxCluster;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(DatalakeEnableSeLinuxHandlerEvent.class), underTest.selector());
    }

    @BeforeEach
    void setUp() {
        handlerEvent = new DatalakeEnableSeLinuxHandlerEvent(1L, "test-resource", "testCrn");
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("test"), new Event<>(handlerEvent));
        assertEquals(FAILED_ENABLE_SELINUX_DATALAKE_EVENT.selector(), response.getSelector());
        assertEquals("test", response.getException().getMessage());
        assertEquals(1L, response.getResourceId());
    }

    @Test
    void testDatalakeEnableSeLinuxHandlerSuccess() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        when(sdxCluster.getId()).thenReturn(1L);
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        when(distroXV1Endpoint.modifySeLinuxByCrn("testCrn", SeLinux.ENFORCING)).thenReturn(flowIdentifier);
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerEvent)));
        DatalakeEnableSeLinuxEvent responseEvent = (DatalakeEnableSeLinuxEvent) response;
        assertEquals(1L, response.getResourceId());
        assertEquals(FINISH_ENABLE_SELINUX_DATALAKE_EVENT.selector(), response.getSelector());
        assertEquals("test-resource", responseEvent.getResourceName());
        assertEquals("testCrn", responseEvent.getResourceCrn());
        assertEquals(1L, responseEvent.getResourceId());
        verify(distroXV1Endpoint).modifySeLinuxByCrn("testCrn", SeLinux.ENFORCING);
        verify(sdxService).getById(1L);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(1L), any(PollingConfig.class), eq("Polling Resize flow"));
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), eq(flowIdentifier));
    }

    @Test
    void testDatalakeEnableSeLinuxHandlerCoreServiceCallFailure() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        doThrow(new RuntimeException("test")).when(distroXV1Endpoint).modifySeLinuxByCrn("testCrn", SeLinux.ENFORCING);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(handlerEvent))));
        assertEquals("test", exception.getMessage());
        verify(distroXV1Endpoint).modifySeLinuxByCrn("testCrn", SeLinux.ENFORCING);
        verify(sdxService).getById(1L);
        verifyNoInteractions(sdxWaitService);
        verifyNoInteractions(cloudbreakFlowService);
    }
}
