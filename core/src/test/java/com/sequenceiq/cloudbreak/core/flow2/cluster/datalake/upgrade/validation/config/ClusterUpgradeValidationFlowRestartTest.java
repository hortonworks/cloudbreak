package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_DATALAKE_REACHABILITY_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState.CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DATALAKE_REACHABILITY_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_SERVICE_VALIDATION_EVENT;
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
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDatalakeReachabilityValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
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
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

class ClusterUpgradeValidationFlowRestartTest {

    private static final Long STACK_ID = 1L;

    @ParameterizedTest(name = "{0}")
    @MethodSource("persistedFlowLogs")
    void restartAcceptsPersistedEvent(String scenario, ClusterUpgradeValidationState source, ClusterUpgradeValidationStateSelectors event, Payload payload,
            ClusterUpgradeValidationState target) throws Exception {
        ClusterUpgradeValidationFlowConfig config = new ClusterUpgradeValidationFlowConfig();
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
        FlowLog log = new FlowLog(STACK_ID, "flow", source.name(), false, StateStatus.PENDING, OperationType.UNKNOWN);
        log.setFlowType(ClassValue.of(ClusterUpgradeValidationFlowConfig.class));
        log.setPayloadType(ClassValue.of(payload.getClass()));
        log.setPayloadJackson(JsonUtil.writeValueAsString(payload));
        log.setVariablesJackson(TypedJsonUtil.writeValueAsStringSilent(new HashMap<>()));
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
        ArgumentCaptor<Event<Payload>> restartedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(event.event()), restartedEvent.capture());
        assertThat(restartedEvent.getValue().getData()).isInstanceOf(payload.getClass());
        assertThat(flow.sendEvent(new FlowEventContext("flow", "user", OperationType.UNKNOWN.name(), null, event.event(), "UPGRADE_VALIDATION",
                restartedEvent.getValue().getData()))).isTrue();
        assertThat(flow.getCurrentState()).isEqualTo(target);
        verify(flowLogService, never()).terminate(any(), any(), any());
        flow.stop();
    }

    private static Stream<Arguments> persistedFlowLogs() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        String imageId = properties.getTargetImageId();
        return Stream.of(
                Arguments.of("persisted before the Data Lake reachability validation existed", CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_STATE,
                        START_CLUSTER_UPGRADE_SERVICE_VALIDATION_EVENT,
                        new ClusterUpgradeFreeIpaStatusValidationFinishedEvent(STACK_ID, imageId, properties),
                        CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE),
                Arguments.of("FreeIPA status validation finished", CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_STATE,
                        START_CLUSTER_UPGRADE_DATALAKE_REACHABILITY_VALIDATION_EVENT,
                        new ClusterUpgradeFreeIpaStatusValidationFinishedEvent(STACK_ID, imageId, properties),
                        CLUSTER_UPGRADE_DATALAKE_REACHABILITY_VALIDATION_STATE),
                Arguments.of("Data Lake reachability validation finished", CLUSTER_UPGRADE_DATALAKE_REACHABILITY_VALIDATION_STATE,
                        START_CLUSTER_UPGRADE_SERVICE_VALIDATION_EVENT,
                        new ClusterUpgradeDatalakeReachabilityValidationFinishedEvent(STACK_ID, imageId, properties),
                        CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE));
    }
}
