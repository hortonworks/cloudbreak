package com.sequenceiq.flow.core.chain;

import static com.sequenceiq.flow.core.FlowConstants.FLOW_CONTEXTPARAMS_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_TRIGGER_USERCRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.config.FlowChainOperationTypeConfig;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowChainLog;

@ExtendWith(MockitoExtension.class)
class FlowChainHandlerTest {

    private static final String FLOW_CHAIN_ID = "flowchainId";

    private static final String FLOW_CHAIN_TYPE = "type";

    private static final String PARENT_FLOW_CHAIN_ID = "parentId";

    private static final String USER_CRN = "userCrn";

    private static final String TRIGGER_VALUE = "triggerValue";

    private static final String SELECTOR = "simpleSelector";

    private static final String SIMPLE_VALUE = "chainKey";

    private static final Long RESOURCE_ID = 123L;

    @Mock
    private FlowEventChainFactory<Payload> factory;

    @Mock
    private FlowChains flowChains;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowStatCache flowStatCache;

    @Mock
    private FlowChainOperationTypeConfig flowChainOperationTypeConfig;

    @InjectMocks
    private FlowChainHandler underTest;

    @Captor
    private ArgumentCaptor<FlowTriggerEventQueue> eventQueueCaptor;

    @Test
    void nestedChainReceivesAnIndependentCopyOfParentContext() {
        Map<Object, Object> parentContext = Map.of("snapshot", "resolved-properties");
        Event<SimpleSelectable> event = chainEvent(Map.of(FlowConstants.FLOW_CHAIN_ID, PARENT_FLOW_CHAIN_ID,
                FLOW_TRIGGER_USERCRN, USER_CRN, FLOW_CONTEXTPARAMS_ID, parentContext));

        underTest.accept(event);

        ArgumentCaptor<Map<Object, Object>> context = ArgumentCaptor.forClass(Map.class);
        verify(flowChains).triggerNextFlow(anyString(), eq(USER_CRN), context.capture(), eq("PROVISION"), eq(Optional.empty()));
        verify(factory).createFlowTriggerEventQueue(eq(event.getData()), eq(context.getValue()));
        assertThat(context.getValue()).containsExactlyInAnyOrderEntriesOf(parentContext).isNotSameAs(parentContext);
        context.getValue().put("child-variable", "value");
        assertThat(parentContext).doesNotContainKey("child-variable");
    }

    @Test
    void chainWithoutIncomingContextStartsWithMutableEmptyVariables() {
        underTest.accept(chainEvent(Map.of(FLOW_TRIGGER_USERCRN, USER_CRN)));

        ArgumentCaptor<Map<Object, Object>> context = ArgumentCaptor.forClass(Map.class);
        verify(flowChains).triggerNextFlow(anyString(), eq(USER_CRN), context.capture(), eq("PROVISION"), eq(Optional.empty()));
        assertThat(context.getValue()).isEmpty();
        context.getValue().put("new-variable", "value");
        assertThat(context.getValue()).containsEntry("new-variable", "value");
    }

    @Test
    void testRestoreFlowChainNotFound() {
        when(flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(FLOW_CHAIN_ID)).thenReturn(Optional.empty());
        underTest.restoreFlowChain(FLOW_CHAIN_ID);
        verifyNoInteractions(flowChains, flowStatCache, flowChainOperationTypeConfig);
    }

    @Test
    void testRestoreFlowChain() {
        FlowChainLog flowChainLog = new FlowChainLog(FLOW_CHAIN_TYPE, FLOW_CHAIN_ID, PARENT_FLOW_CHAIN_ID,
                "[{\"@type\":\"" +
                        SimpleSelectable.class.getName() + "\",\"resourceId\":" + RESOURCE_ID + ",\"key\":\"" + SIMPLE_VALUE + "\"," +
                        "\"selector\":\"" + SELECTOR + "\"}]",
                USER_CRN,
                "{\"@type\":\"" + SimpleSelectable.class.getName() + "\",\"key\":\"" + TRIGGER_VALUE + "\"}");
        when(flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(FLOW_CHAIN_ID)).thenReturn(Optional.of(flowChainLog));
        when(flowChainOperationTypeConfig.getFlowTypeOperationTypeMap()).thenReturn(Map.of(FLOW_CHAIN_TYPE, OperationType.PROVISION));
        underTest.restoreFlowChain(FLOW_CHAIN_ID);

        verify(flowChains).putFlowChain(eq(FLOW_CHAIN_ID), eq(PARENT_FLOW_CHAIN_ID), eventQueueCaptor.capture());
        FlowTriggerEventQueue eventQueue = eventQueueCaptor.getValue();
        assertThat(eventQueue.getFlowChainName()).isEqualTo(FLOW_CHAIN_TYPE);
        assertThat(eventQueue.getParentFlowChainId()).isEqualTo(PARENT_FLOW_CHAIN_ID);
        assertThat(eventQueue.getTriggerEvent()).isInstanceOf(SimpleSelectable.class);
        assertThat(((SimpleSelectable) eventQueue.getTriggerEvent()).getKey()).isEqualTo(TRIGGER_VALUE);
        assertThat(eventQueue.getQueue()).contains(new SimpleSelectable(SELECTOR, RESOURCE_ID, SIMPLE_VALUE));
        verify(flowStatCache).putByFlowChainId(FLOW_CHAIN_ID, RESOURCE_ID, "PROVISION", true);
    }

    private Event<SimpleSelectable> chainEvent(Map<String, Object> headers) {
        SimpleSelectable trigger = new SimpleSelectable(SELECTOR, RESOURCE_ID, TRIGGER_VALUE);
        when(factory.getFlowOperationType()).thenReturn(OperationType.PROVISION);
        when(factory.createFlowTriggerEventQueue(trigger)).thenReturn(
                new FlowTriggerEventQueue(FLOW_CHAIN_TYPE, trigger, new ConcurrentLinkedQueue<>()));
        when(factory.createFlowTriggerEventQueue(eq(trigger), anyMap())).thenCallRealMethod();
        ReflectionTestUtils.setField(underTest, "flowChainConfigMap", Map.of(SELECTOR, factory));
        Event<SimpleSelectable> event = new Event<>(new Event.Headers(headers), trigger);
        event.setKey(SELECTOR);
        return event;
    }

    static class SimpleSelectable implements Selectable {

        private final String selector;

        private final Long resourceId;

        private final String key;

        @JsonCreator
        SimpleSelectable(@JsonProperty("selector") String selector,
                @JsonProperty("resourceId") Long resourceId,
                @JsonProperty("key") String key) {
            this.selector = selector;
            this.resourceId = resourceId;
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public Long getResourceId() {
            return resourceId;
        }

        @Override
        public String toString() {
            return "Simple{" +
                    "key='" + key + '\'' +
                    '}';
        }

        @Override
        public String selector() {
            return selector;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SimpleSelectable)) {
                return false;
            }
            SimpleSelectable that = (SimpleSelectable) o;
            return Objects.equals(selector, that.selector) && Objects.equals(key, that.key) && Objects.equals(resourceId, that.resourceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(selector, key, resourceId);
        }
    }
}
