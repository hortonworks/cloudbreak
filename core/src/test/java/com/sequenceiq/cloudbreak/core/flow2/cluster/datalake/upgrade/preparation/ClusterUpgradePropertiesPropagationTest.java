package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.PREPARE_PARCEL_SETTINGS_EVENT;
import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CONTEXTPARAMS_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_FINAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.AbstractClusterUpgradeValidationAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeValidationActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesFactory;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowFinalizeAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

class ClusterUpgradePropertiesPropagationTest {

    private final EventBus eventBus = mock(EventBus.class);

    private final FlowRegister runningFlows = mock(FlowRegister.class);

    private final ErrorHandlerAwareReactorEventFactory eventFactory = new ErrorHandlerAwareReactorEventFactory();

    private final ClusterUpgradePropertiesFactory propertiesFactory = mock(ClusterUpgradePropertiesFactory.class);

    private final ClusterUpgradePropertiesResolver resolver = new ClusterUpgradePropertiesResolver(propertiesFactory);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preparationReusesValidationPropertiesAfterFlowContextIsRestored(boolean validationSkipped) throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "target", OsType.RHEL9, "x86_64",
                new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2").withParcel("target-parcel"),
                Set.of(new ClouderaManagerProduct().withName("FLINK").withVersion("1.20").withParcel("flink-parcel")),
                new ClouderaManagerRepo().withVersion("7.13.1").withBuildNumber("123").withBaseUrl("target-repo"));
        Map<Object, Object> variables = finishValidation(properties, validationSkipped);
        FlowFinalizeAction finalizeAction = new FlowFinalizeAction();
        initAction(finalizeAction);
        new AbstractActionTestSupport<>(finalizeAction).doExecute(new CommonContext(new FlowParameters("validation", "user")),
                new ClusterUpgradeValidationFinishedEvent(1L, properties.targetImageId(), properties), variables);
        ArgumentCaptor<Event<?>> finalEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(FLOW_FINAL), finalEvent.capture());

        FlowLog flowLog = new FlowLog();
        flowLog.setVariablesJackson(TypedJsonUtil.writeValueAsStringSilent(finalEvent.getValue().getHeaders().get(FLOW_CONTEXTPARAMS_ID)));
        Map<Object, Object> restoredVariables = FlowLogUtil.deserializeVariables(flowLog);
        ClusterUpgradePreparationTriggerEvent trigger = new ClusterUpgradePreparationTriggerEvent(1L, null,
                new ImageChangeDto(1L, properties.targetImageId()), "7.3.2", OsType.RHEL8, null);
        trigger = JsonUtil.readValue(JsonUtil.writeValueAsString(trigger), ClusterUpgradePreparationTriggerEvent.class);
        FlowChains flowChains = new FlowChains();
        ReflectionTestUtils.setField(flowChains, "eventBus", eventBus);
        ReflectionTestUtils.setField(flowChains, "eventFactory", eventFactory);
        flowChains.putFlowChain("chain", null, new FlowTriggerEventQueue("upgrade", trigger, new ConcurrentLinkedQueue<>(List.of(trigger))));
        flowChains.triggerNextFlow("chain", "user", restoredVariables, "UPGRADE", Optional.empty());
        ArgumentCaptor<Event<ClusterUpgradePreparationTriggerEvent>> preparationEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(trigger.selector()), preparationEvent.capture());

        ClusterUpgradePreparationActions preparationActions = new ClusterUpgradePreparationActions();
        initServices(preparationActions);
        ReflectionTestUtils.setField(preparationActions, "clusterUpgradePropertiesResolver", resolver);
        AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent> preparationAction =
                (AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent>) preparationActions.initClusterUpgradePreparation();
        initAction(preparationAction);
        new AbstractActionTestSupport<>(preparationAction).doExecute(context("preparation"), preparationEvent.getValue().getData(),
                preparationEvent.getValue().getHeaders().get(FLOW_CONTEXTPARAMS_ID));

        ArgumentCaptor<Event<ClusterUpgradeParcelSettingsPreparationEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(PREPARE_PARCEL_SETTINGS_EVENT.event()), emitted.capture());
        ClusterUpgradeProperties restored = emitted.getValue().getData().getClusterUpgradeProperties();
        assertThat(restored).isSameAs(restoredVariables.get(FLOW_VARIABLE_NAME));
        assertThat(restored.targetImage().imageId()).isEqualTo(properties.targetImageId());
        assertThat(restored.targetImage().cdhParcel().getParcel()).isEqualTo("target-parcel");
        assertThat(restored.targetImage().preWarmParcels()).extracting(ClouderaManagerProduct::getParcel).containsExactly("flink-parcel");
        assertThat(restored.targetImage().clouderaManagerRepo().getBaseUrl()).isEqualTo("target-repo");
        assertThat(restored.options()).isEqualTo(properties.options());
        verifyNoInteractions(propertiesFactory);
    }

    private Map<Object, Object> finishValidation(ClusterUpgradeProperties properties, boolean validationSkipped) throws Exception {
        ClusterUpgradeValidationActions validationActions = new ClusterUpgradeValidationActions();
        initServices(validationActions);
        AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationFinishedEvent> action =
                (AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationFinishedEvent>) validationActions.clusterUpgradeValidationFinished();
        initAction(action);
        ReflectionTestUtils.setField(action, "clusterUpgradePropertiesResolver", resolver);
        ClusterUpgradeValidationFinishedEvent payload = new ClusterUpgradeValidationFinishedEvent(1L, properties.targetImageId(), properties,
                validationSkipped ? new IllegalStateException("Service validation unavailable") : null);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(context("validation"), payload, variables);
        return variables;
    }

    private StackContext context(String flowId) {
        return new StackContext(new FlowParameters(flowId, "user"), mock(StackDto.class), null, null, null);
    }

    private void initServices(Object actions) {
        ReflectionTestUtils.setField(actions, "stackUpdater", mock(StackUpdater.class));
        ReflectionTestUtils.setField(actions, "messagesService", mock(CloudbreakMessagesService.class));
        ReflectionTestUtils.setField(actions, "cloudbreakEventService", mock(CloudbreakEventService.class));
    }

    private void initAction(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(action, "eventBus", eventBus);
        ReflectionTestUtils.setField(action, "reactorEventFactory", eventFactory);
    }
}
