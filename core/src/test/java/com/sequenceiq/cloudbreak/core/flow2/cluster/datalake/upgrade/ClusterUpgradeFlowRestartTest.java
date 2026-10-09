package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowEventContext;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

class ClusterUpgradeFlowRestartTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("persistedTransitions")
    void restartRestoresPropertiesAndAcceptsPersistedEvent(ClusterUpgradeState source, ClusterUpgradeState target, ClusterUpgradeEvent event)
            throws Exception {
        ClusterUpgradeFlowConfig config = new ClusterUpgradeFlowConfig();
        EventBus eventBus = mock(EventBus.class);
        FillInMemoryStateStoreRestartAction restartAction = spy(new FillInMemoryStateStoreRestartAction());
        doNothing().when(restartAction).doBeforeRestart(any(), any());
        ReflectionTestUtils.setField(restartAction, "eventBus", eventBus);
        ReflectionTestUtils.setField(restartAction, "eventFactory", new ErrorHandlerAwareReactorEventFactory());
        ApplicationContext applicationContext = mock(ApplicationContext.class, invocation -> {
            Object bean = invocation.getArgument(0);
            if (bean == FlowEventListener.class) {
                return mock(FlowEventListener.class);
            }
            if (bean == FlowEventCommonListener.class) {
                return mock(FlowEventCommonListener.class);
            }
            if ("FillInMemoryStateStoreRestartAction".equals(bean)) {
                return restartAction;
            }
            return mock(AbstractAction.class);
        });
        ReflectionTestUtils.setField(config, "applicationContext", applicationContext);
        config.init();
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "base-image-version", OsType.RHEL9, "x86_64",
                new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2-build").withParcel("runtime-repo").withCsd(List.of("runtime-csd")),
                Set.of(new ClouderaManagerProduct().withName("FLINK").withVersion("1.20").withParcel("flink-repo")),
                new ClouderaManagerRepo().withVersion("7.13.2").withBaseUrl("cm-repo"));
        Payload payload = new ClusterUpgradeTriggerEvent(event.event(), 1L, null, properties.targetImageId(), true,
                properties.toCurrentCloudImage(), properties);
        FlowLog log = new FlowLog(1L, "flow", source.name(), false, StateStatus.PENDING, OperationType.UNKNOWN);
        log.setFlowType(ClassValue.of(ClusterUpgradeFlowConfig.class));
        log.setPayloadType(ClassValue.of(payload.getClass()));
        log.setPayloadJackson(JsonUtil.writeValueAsString(payload));
        log.setVariablesJackson(TypedJsonUtil.writeValueAsStringSilent(new HashMap<>(Map.of(FLOW_VARIABLE_NAME, properties))));
        log.setNextEvent(event.event());
        Flow2Handler flowHandler = new Flow2Handler();
        FlowLogService flowLogService = mock(FlowLogService.class);
        FlowRegister runningFlows = mock(FlowRegister.class);
        ReflectionTestUtils.setField(flowHandler, "flowLogService", flowLogService);
        ReflectionTestUtils.setField(flowHandler, "flowChainLogService", mock(FlowChainLogService.class));
        ReflectionTestUtils.setField(flowHandler, "flowConfigs", List.of(config));
        ReflectionTestUtils.setField(flowHandler, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(flowHandler, "flowStatCache", mock(FlowStatCache.class));

        flowHandler.restartFlow(log);

        ArgumentCaptor<Flow> restoredFlow = ArgumentCaptor.forClass(Flow.class);
        verify(runningFlows).put(restoredFlow.capture(), eq(null));
        Flow flow = restoredFlow.getValue();
        assertThat(flow.getCurrentState()).isEqualTo(source);
        assertThat(flow.getVariables().get(FLOW_VARIABLE_NAME)).usingRecursiveComparison().isEqualTo(properties);
        ArgumentCaptor<Event<Payload>> restartedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(event.event()), restartedEvent.capture());
        assertThat(flow.sendEvent(new FlowEventContext("flow", "user", OperationType.UNKNOWN.name(), null, event.event(), "UPGRADE",
                restartedEvent.getValue().getData()))).isTrue();
        assertThat(flow.getCurrentState()).isEqualTo(target);
        verify(flowLogService, never()).terminate(any(), any(), any());
        flow.stop();
    }

    private static Stream<Arguments> persistedTransitions() {
        return new ClusterUpgradeFlowConfig().getTransitions().stream()
                .map(transition -> Arguments.of(transition.getSource(), transition.getTarget(), transition.getEvent()));
    }
}
