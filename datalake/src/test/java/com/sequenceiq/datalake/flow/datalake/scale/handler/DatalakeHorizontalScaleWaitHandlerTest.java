package com.sequenceiq.datalake.flow.datalake.scale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;

@ExtendWith(MockitoExtension.class)
class DatalakeHorizontalScaleWaitHandlerTest {

    private static final Long SDX_ID = 1L;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private SdxService sdxService;

    @Mock
    private EventBus eventBus;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private DatalakeHorizontalScaleWaitHandler underTest;

    @Test
    void whenDatalakeHorizontalScaleFinishedSuccessEventShouldBeEmitted() {
        Payload payload = mock(Payload.class);
        DatalakeHorizontalScaleRequest datalakeHorizontalScaleRequest = new DatalakeHorizontalScaleRequest();
        DatalakeHorizontalScaleFlowEvent horizontalScaleFlowEvent = new DatalakeHorizontalScaleFlowEvent("selector",
                1L, "dl", "crn:crn",
                "userId",
                datalakeHorizontalScaleRequest,
                new BigDecimal(1), new Exception("TestException"));
        Event<DatalakeHorizontalScaleFlowEvent> event = new Event<>(horizontalScaleFlowEvent);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        underTest.accept(event);
        final ArgumentCaptor<DatalakeHorizontalScaleFlowEvent> eventSent = ArgumentCaptor.forClass(DatalakeHorizontalScaleFlowEvent.class);
        final ArgumentCaptor<Event.Headers> sentEvent = ArgumentCaptor.forClass(Event.Headers.class);
        verify(eventSender, times(1)).sendEvent(eventSent.capture(), sentEvent.capture());
        assertEquals("DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_EVENT", eventSent.getValue().selector());
    }
}