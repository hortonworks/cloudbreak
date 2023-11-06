package com.sequenceiq.flow.reactor.config;

import static org.mockito.Mockito.timeout;
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
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventBusConfig.class})
class EventBusConfigTest {

    @Inject
    private EventBus eventBus;

    @MockBean
    private FlowLogDBService flowLogDBService;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void unknownMessage() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("123");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.of(flowLog));
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.notify("notexist", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1))
                .closeFlow("123", "Unhanded event: com.sequenceiq.cloudbreak.eventbus.Event");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void uncaughtError() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("123");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.of(flowLog));
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.on("exampleselector", (Consumer<Event<? extends Payload>>) event -> {
            throw new RuntimeException("uncaught exception");
        });
        eventBus.notify("exampleselector", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1))
                .closeFlow("123", "Unhandled exception happened in flow, type: java.lang.RuntimeException, message: uncaught exception");
    }
}