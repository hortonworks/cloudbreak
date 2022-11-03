package com.sequenceiq.flow.reactor.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventBusConfig.class})
class EventBusConfigTest {

    @Inject
    private EventBus eventBus;

    @MockBean
    private ApplicationFlowInformation applicationFlowInformation;

    @MockBean
    private FlowLogDBService flowLogDBService;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void unknownMessageButFlowFound() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("123");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.of(flowLog));
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.notify("notexist", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1)).findFirstByFlowIdOrderByCreatedDesc("123");
        verify(applicationFlowInformation, timeout(1000).times(1)).handleFlowFail(flowLog);
        verify(flowLogDBService, timeout(1000).times(1)).updateLastFlowLogStatus(flowLog, true);
        verify(flowLogDBService, timeout(1000).times(1)).finalize(flowLog.getFlowId());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void unknownMessageFlowNotFound() {
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.empty());
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.notify("notexist", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1)).findFirstByFlowIdOrderByCreatedDesc("123");
        verify(applicationFlowInformation, times(0)).handleFlowFail(any());
        verify(flowLogDBService, times(0)).updateLastFlowLogStatus(any(), anyBoolean());
        verify(flowLogDBService, times(0)).finalize(any());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void uncaughtErrorButFlowFound() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("123");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.of(flowLog));
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.on("exampleselector", (Consumer<Event<? extends Payload>>) event -> {
            throw new RuntimeException("uncaught exception");
        });
        eventBus.notify("exampleselector", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1)).findFirstByFlowIdOrderByCreatedDesc("123");
        verify(applicationFlowInformation, timeout(1000).times(1)).handleFlowFail(flowLog);
        verify(flowLogDBService, timeout(1000).times(1)).updateLastFlowLogStatus(flowLog, true);
        verify(flowLogDBService, timeout(1000).times(1)).finalize(flowLog.getFlowId());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void uncaughtErrorButFlowNotFound() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("123");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.empty());
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.on("exampleselector", (Consumer<Event<? extends Payload>>) event -> {
            throw new RuntimeException("uncaught exception");
        });
        eventBus.notify("exampleselector", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1)).findFirstByFlowIdOrderByCreatedDesc("123");
        verify(applicationFlowInformation, times(0)).handleFlowFail(any());
        verify(flowLogDBService, times(0)).updateLastFlowLogStatus(any(), anyBoolean());
        verify(flowLogDBService, times(0)).finalize(any());
    }

}