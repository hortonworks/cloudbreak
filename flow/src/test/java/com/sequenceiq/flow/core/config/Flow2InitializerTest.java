package com.sequenceiq.flow.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldFlowConfig;

@ExtendWith(MockitoExtension.class)
class Flow2InitializerTest {

    @Spy
    private List<AbstractFlowConfiguration<?, ?>> flowConfigs = new ArrayList<>();

    @Mock
    private EventBus reactor;

    @Mock
    private Flow2Handler flow2Handler;

    @Spy
    private List<FlowEventChainFactory<?>> flowChainFactories = new ArrayList<>();

    @Mock
    private FlowEventChainFactory<?> flowEventChainFactory;

    @InjectMocks
    private Flow2Initializer underTest;

    @Test
    void testInitialize() {
        flowConfigs.add(new HelloWorldFlowConfig());
        given(flowEventChainFactory.initEvent()).willReturn("OTHER_SELECTOR");
        flowChainFactories.add(flowEventChainFactory);
        underTest.init();
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(reactor, times(9)).on(selectorCaptor.capture(), any(Consumer.class));
        assertEquals(
                List.of("FLOWFINAL",
                        "FLOWCANCEL",
                        "HELLOWORLD_FIRST_STEP_FINISHED_EVENT",
                        "HELLOWORLD_FAILHANDLED_EVENT",
                        "HELLOWORLD_SOMETHING_WENT_WRONG",
                        "HELLOWORLD_SECOND_STEP_FINISHED_EVENT",
                        "HELLOWORLD_FIRST_STEP_WENT_WRONG_EVENT",
                        "FINALIZE_HELLOWORLD_EVENT",
                        "HELLOWORLD_TRIGGER_EVENT"),
                selectorCaptor.getAllValues());
    }

    @Test
    void testInitializeFailsWithFlowSelectorIsFlowChainSelector() {
        flowConfigs.add(new HelloWorldFlowConfig());
        given(flowEventChainFactory.initEvent()).willReturn(HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT.name());
        flowChainFactories.add(flowEventChainFactory);
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> underTest.init());
        assertEquals("HELLOWORLD_TRIGGER_EVENT is a flow selector and a flow chain selector. It should be only in one category.",
                runtimeException.getMessage());
        verify(reactor, times(0)).on(any(), any(Consumer.class));
    }
}
