package com.sequenceiq.flow.core.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.api.model.operation.OperationType;
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
    void testRestoreFlowChainNotFound() {
        when(flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(FLOW_CHAIN_ID)).thenReturn(Optional.empty());
        underTest.restoreFlowChain(FLOW_CHAIN_ID);
        verifyNoInteractions(flowChains, flowStatCache, flowChainOperationTypeConfig);
    }

    @ParameterizedTest(name = "use Jackson = {0}")
    @ValueSource(booleans = { false, true })
    void testRestoreFlowChain(boolean useJackson) {
        FlowChainLog flowChainLog = new FlowChainLog(FLOW_CHAIN_TYPE, FLOW_CHAIN_ID, PARENT_FLOW_CHAIN_ID,
                "{\"@type\":\"java.util.concurrent.ConcurrentLinkedQueue\",\"@items\":[{\"@type\":\"" +
                        SimpleSelectable.class.getName() + "\",\"resourceId\":" + RESOURCE_ID + ",\"key\":\"" + SIMPLE_VALUE + "\"," +
                        "\"selector\":\"" + SELECTOR + "\"}]}",
                useJackson
                        ? "[{\"@type\":\"" +
                            SimpleSelectable.class.getName() + "\",\"resourceId\":" + RESOURCE_ID + ",\"key\":\"" + SIMPLE_VALUE + "\"," +
                            "\"selector\":\"" + SELECTOR + "\"}]"
                        : null,
                USER_CRN,
                "{\"@type\":\"" + SimpleSelectable.class.getName() + "\",\"key\":\"" + TRIGGER_VALUE + "\"}",
                useJackson
                        ? "{\"@type\":\"" + SimpleSelectable.class.getName() + "\",\"key\":\"" + TRIGGER_VALUE + "\"}"
                        : null);
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
