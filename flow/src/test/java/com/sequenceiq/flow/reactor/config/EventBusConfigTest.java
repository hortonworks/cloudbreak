package com.sequenceiq.flow.reactor.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EventBusConfig.class, EventBusConfigTest.TestConfig.class})
class EventBusConfigTest {

    @Inject
    private EventBus eventBus;

    @MockBean
    private FlowLogDBService flowLogDBService;

    @MockBean
    private MeterRegistry meterRegistry;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void unknownMessage() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("123");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc("123")).thenReturn(Optional.of(flowLog));
        Event.Headers headers = new Event.Headers();
        headers.set("FLOW_ID", "123");
        eventBus.notify("notexist", new Event<>(headers, null));
        verify(flowLogDBService, timeout(2000).times(1))
                .closeFlowOnError("123", "Unhanded event: com.sequenceiq.cloudbreak.eventbus.Event");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void uncaughtError() {
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
                .closeFlowOnError("123", "Unhandled exception happened in flow, type: java.lang.RuntimeException, message: uncaught exception");
    }

    @Configuration
    static class TestConfig {

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any()))
                    .thenReturn(Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}
