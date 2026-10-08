package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradePreparationActionsTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private ClusterUpgradePropertiesResolver resolver;

    @InjectMocks
    private ClusterUpgradePreparationActions underTest;

    @Test
    void testInitForwardsResolvedProperties() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        ClusterUpgradePreparationTriggerEvent trigger = new ClusterUpgradePreparationTriggerEvent(STACK_ID, null,
                new ImageChangeDto(STACK_ID, properties.targetImageId()), properties.runtimeVersion(), properties.currentOsType(), properties);
        AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent> action =
                (AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent>) underTest.initClusterUpgradePreparation();
        initAction(action);
        ArgumentCaptor<ClusterUpgradeParcelSettingsPreparationEvent> emitted = ArgumentCaptor.forClass(ClusterUpgradeParcelSettingsPreparationEvent.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));

        new AbstractActionTestSupport<>(action).doExecute(context(), trigger, new HashMap<>(Map.of(
                FLOW_VARIABLE_NAME, ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18"))));

        verifyNoInteractions(resolver);
        assertThat(emitted.getValue().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emitted.getValue().getImageChangeDto().getImageId()).isEqualTo(properties.targetImage().imageId());
        assertThat(JsonUtil.readTree(JsonUtil.writeValueAsString(emitted.getValue())).get("currentOsType").asText())
                .isEqualTo(properties.currentImage().osType().name());
    }

    @Test
    void testLegacyTriggerWithoutSharedPropertiesUsesResolver() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        ClusterUpgradePreparationTriggerEvent trigger = new ClusterUpgradePreparationTriggerEvent(STACK_ID, null,
                new ImageChangeDto(STACK_ID, properties.targetImageId()), "7.3.2", null, null);
        when(resolver.resolve(trigger)).thenReturn(properties);
        AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent> action =
                (AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent>) underTest.initClusterUpgradePreparation();
        initAction(action);
        ArgumentCaptor<ClusterUpgradeParcelSettingsPreparationEvent> emitted = ArgumentCaptor.forClass(ClusterUpgradeParcelSettingsPreparationEvent.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));

        new AbstractActionTestSupport<>(action).doExecute(context(), trigger, new HashMap<>());

        assertThat(emitted.getValue().getClusterUpgradeProperties()).isSameAs(properties);
    }

    @Test
    void testDownloadAndDistributionActionsPreservePropertiesAndPreparedProducts() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        Set<ClouderaManagerProduct> products = Set.of(new ClouderaManagerProduct().withName("CDH").withParcel("current-os-url"));
        ClusterUpgradePreparationEvent payload = new ClusterUpgradePreparationEvent("selector", STACK_ID, products, properties.targetImageId(), properties);
        ArgumentCaptor<ClusterUpgradePreparationEvent> emitted = ArgumentCaptor.forClass(ClusterUpgradePreparationEvent.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));
        List<Action<?, ?>> actions = List.of(underTest.clusterUpgradePreparationCmPackageDownload(), underTest.clusterUpgradePreparationParcelDownload(),
                underTest.clusterUpgradePreparationParcelDistribution(), underTest.clusterUpgradePreparationCsdPackageDownload());

        for (Action<?, ?> action : actions) {
            initAction(action);
            new AbstractActionTestSupport<>((AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationEvent>) action)
                    .doExecute(context(), payload, new HashMap<>());
        }

        assertThat(emitted.getAllValues()).hasSize(4).allSatisfy(event -> {
            assertThat(event.getClusterUpgradeProperties()).isSameAs(properties);
            assertThat(event.getClouderaManagerProducts()).isSameAs(products);
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "requested-runtime")
    void preparationNotificationPreservesRequestedRuntimeOrUsesCatalogImageVersion(String requestedRuntime) throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("package-runtime", "image-version", OsType.RHEL8,
                "x86_64", null, Set.of(), null);
        ClusterUpgradePreparationTriggerEvent trigger = new ClusterUpgradePreparationTriggerEvent(STACK_ID, null,
                new ImageChangeDto(STACK_ID, properties.targetImageId()), requestedRuntime, OsType.RHEL8, null);
        AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent> action =
                (AbstractClusterUpgradePreparationAction<ClusterUpgradePreparationTriggerEvent>) underTest.initClusterUpgradePreparation();
        initAction(action);

        new AbstractActionTestSupport<>(action).doExecute(context(), trigger, new HashMap<>(Map.of(FLOW_VARIABLE_NAME, properties)));

        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_PREPARATION_STARTED,
                List.of(requestedRuntime == null ? "image-version" : requestedRuntime, properties.targetImageId()));
        verifyNoInteractions(resolver);
    }

    private StackContext context() {
        return new StackContext(mock(FlowParameters.class), mock(StackDto.class), null, null, null);
    }

    private void initAction(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
