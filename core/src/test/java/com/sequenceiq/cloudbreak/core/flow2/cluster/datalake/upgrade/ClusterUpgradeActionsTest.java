package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.ClusterComponentUpdateService;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.ImageComponentUpdaterService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeActionsTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private ClusterUpgradeService clusterUpgradeService;

    @Mock
    private ImageComponentUpdaterService imageComponentUpdaterService;

    @Mock
    private ClusterUpgradePropertiesResolver resolver;

    @Mock
    private ClusterUpgradeTargetImageService targetImageService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ClusterComponentUpdateService clusterComponentUpdateService;

    @InjectMocks
    private ClusterUpgradeActions underTest;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failedActionPassesPropertiesToCmSyncWhenAvailable(boolean withProperties) throws Exception {
        ClusterUpgradeProperties properties = withProperties ? properties() : null;
        Map<Object, Object> variables = new HashMap<>();
        if (properties != null) {
            variables.put(FLOW_VARIABLE_NAME, properties);
        }
        AbstractClusterUpgradeAction<ClusterUpgradeFailedEvent> action =
                (AbstractClusterUpgradeAction<ClusterUpgradeFailedEvent>) underTest.clusterUpgradeFailedAction();
        initAction(action);
        ArgumentCaptor<ClusterUpgradeFailedRequest> emitted = ArgumentCaptor.forClass(ClusterUpgradeFailedRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));
        ClusterUpgradeFailedEvent failure = new ClusterUpgradeFailedEvent(STACK_ID, new RuntimeException("failed"),
                DetailedStackStatus.CLUSTER_UPGRADE_FAILED);

        new AbstractActionTestSupport<>(action).doExecute(context(), failure, variables);

        assertThat(emitted.getValue().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emitted.getValue().getException()).isSameAs(failure.getException());
        verifyNoInteractions(resolver);
    }

    @Test
    void testInitUsesResolvedRuntimeInsteadOfBaseImageVersion() throws Exception {
        ClusterUpgradeProperties properties = properties();
        Image originalImage = properties.toCurrentCloudImage();
        ClusterUpgradeTriggerEvent trigger = new ClusterUpgradeTriggerEvent("selector", STACK_ID, null, "targetImageId", true, originalImage, properties);
        AbstractClusterUpgradeAction<ClusterUpgradeTriggerEvent> action =
                (AbstractClusterUpgradeAction<ClusterUpgradeTriggerEvent>) underTest.initClusterUpgrade();
        initAction(action);
        ReflectionTestUtils.setField(action, "clusterUpgradeTargetImageService", targetImageService);
        ArgumentCaptor<ClusterUpgradeInitRequest> emitted = ArgumentCaptor.forClass(ClusterUpgradeInitRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));
        Map<Object, Object> variables = new HashMap<>();

        new AbstractActionTestSupport<>(action).doExecute(context(), trigger, variables);

        assertThat(variables.get(FLOW_VARIABLE_NAME)).isSameAs(properties);
        assertThat(variables).doesNotContainKey(AbstractClusterUpgradeAction.TARGET_IMAGE);
        assertThat(emitted.getValue().getTargetRuntimeVersion()).isEqualTo("7.3.2");
        assertThat(emitted.getValue().getTargetOsType()).isEqualTo(OsType.RHEL9);
        assertThat(emitted.getValue().getOriginalOsType()).isEqualTo(OsType.RHEL8);
        verify(imageComponentUpdaterService).updateComponentsForUpgrade(properties, STACK_ID);
        verify(targetImageService).saveImage(STACK_ID, properties);
        verify(clusterUpgradeService).initUpgradeCluster(STACK_ID, properties, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void initReusesSharedPropertiesAndOriginalOsAfterImageUpdate(boolean propertiesOnPayload) throws Exception {
        ClusterUpgradeProperties properties = properties();
        ClusterUpgradeTriggerEvent trigger = new ClusterUpgradeTriggerEvent("selector", STACK_ID, null, properties.targetImageId(),
                false, properties.toCloudImage(), propertiesOnPayload ? properties : null);
        AbstractClusterUpgradeAction<ClusterUpgradeTriggerEvent> action =
                (AbstractClusterUpgradeAction<ClusterUpgradeTriggerEvent>) underTest.initClusterUpgrade();
        initAction(action);
        ReflectionTestUtils.setField(action, "clusterUpgradeTargetImageService", targetImageService);
        ArgumentCaptor<ClusterUpgradeInitRequest> emitted = ArgumentCaptor.forClass(ClusterUpgradeInitRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));
        Map<Object, Object> variables = new HashMap<>(Map.of(FLOW_VARIABLE_NAME, properties));

        new AbstractActionTestSupport<>(action).doExecute(context(), trigger, variables);

        verifyNoInteractions(resolver);
        assertThat(variables.get(FLOW_VARIABLE_NAME)).isSameAs(properties);
        assertThat(variables.get(AbstractClusterUpgradeAction.ROLLING_UPGRADE_ENABLED)).isEqualTo(properties.rollingUpgradeEnabled());
        assertThat(emitted.getValue().getOriginalOsType()).isEqualTo(OsType.RHEL8);
        assertThat(emitted.getValue().getTargetOsType()).isEqualTo(OsType.RHEL9);
        assertThat(emitted.getValue().getTargetRuntimeVersion()).isEqualTo("7.3.2");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testManagerUpgradeReusesProperties(boolean prewarmed) throws Exception {
        ClusterUpgradeProperties properties = properties(prewarmed);
        Map<Object, Object> variables = variables(properties);
        AbstractClusterUpgradeAction<ClusterUpgradeInitSuccess> action =
                (AbstractClusterUpgradeAction<ClusterUpgradeInitSuccess>) underTest.upgradeClusterManager();
        initAction(action);
        ArgumentCaptor<ClusterManagerUpgradeRequest> emitted = ArgumentCaptor.forClass(ClusterManagerUpgradeRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));

        new AbstractActionTestSupport<>(action).doExecute(context(), new ClusterUpgradeInitSuccess(STACK_ID, Set.of(), OsType.RHEL8), variables);

        assertThat(emitted.getValue().getTargetRuntimeVersion()).isEqualTo("7.3.2");
        verify(imageComponentUpdaterService).updateComponentsForUpgrade(properties, STACK_ID);
        verifyNoInteractions(resolver);
    }

    @Test
    void testManagementServicesReceivesOnlyUpgradeProperties() throws Exception {
        ClusterUpgradeProperties properties = properties();
        Map<Object, Object> variables = variables(properties);
        variables.put(AbstractClusterUpgradeAction.ORIGINAL_IMAGE, properties.toCurrentCloudImage());
        AbstractClusterUpgradeAction<ClusterUpgradeSuccess> action =
                (AbstractClusterUpgradeAction<ClusterUpgradeSuccess>) underTest.configureManagementServicesAction();
        initAction(action);
        ArgumentCaptor<ConfigureClusterManagerManagementServicesRequest> emitted =
                ArgumentCaptor.forClass(ConfigureClusterManagerManagementServicesRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), emitted.capture())).thenReturn(mock(Event.class));

        new AbstractActionTestSupport<>(action).doExecute(context(), new ClusterUpgradeSuccess(STACK_ID), variables);

        assertThat(emitted.getValue().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emitted.getValue().getOriginalImage()).isNull();
        assertThat(emitted.getValue().getCurrentImage()).isNull();
        assertThat(emitted.getValue().getTargetStatedImage()).isNull();
        verifyNoInteractions(resolver);
    }

    @Test
    void testLegacyFlowVariablesAreResolvedOnceAfterJsonRestore() throws Exception {
        ClusterUpgradeProperties properties = properties();
        StatedImage targetImage = StatedImage.statedImage(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(properties.targetImageId()).build(), "persisted-catalog-url", "persisted-catalog");
        Map<Object, Object> variables = new HashMap<>();
        Image originalImage = properties.toCurrentCloudImage();
        variables.put(AbstractClusterUpgradeAction.ORIGINAL_IMAGE, originalImage);
        variables.put(AbstractClusterUpgradeAction.TARGET_IMAGE, targetImage);
        variables.put(AbstractClusterUpgradeAction.ROLLING_UPGRADE_ENABLED, true);
        when(resolver.resolve(eq(STACK_ID), any(Image.class), any(StatedImage.class), eq(true))).thenReturn(properties);
        variables = TypedJsonUtil.readValue(TypedJsonUtil.writeValueAsStringSilent(variables), Map.class);
        AbstractClusterUpgradeAction<?> action = (AbstractClusterUpgradeAction<?>) underTest.upgradeClusterManager();
        initAction(action);

        assertThat(action.getUpgradeProperties(variables, STACK_ID)).isSameAs(properties);
        assertThat(action.getUpgradeProperties(variables, STACK_ID)).isSameAs(properties);
        ArgumentCaptor<Image> original = ArgumentCaptor.forClass(Image.class);
        ArgumentCaptor<StatedImage> target = ArgumentCaptor.forClass(StatedImage.class);
        verify(resolver).resolve(eq(STACK_ID), original.capture(), target.capture(), eq(true));
        assertThat(original.getValue().getOsType()).isEqualTo("redhat8");
        assertThat(target.getValue().getImageCatalogName()).isEqualTo("persisted-catalog");
        assertThat(target.getValue().getImageCatalogUrl()).isEqualTo("persisted-catalog-url");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void finishedUpgradeUsesSnapshotAndRollingOptionsForBothImageTypes(boolean prewarmed) throws Exception {
        ClusterUpgradeProperties properties = properties(prewarmed);
        Map<Object, Object> variables = variables(properties);
        variables.put(AbstractClusterUpgradeAction.ROLLING_UPGRADE_ENABLED, false);
        AbstractClusterUpgradeAction<ConfigureClusterManagerManagementServicesSuccess> action =
                (AbstractClusterUpgradeAction<ConfigureClusterManagerManagementServicesSuccess>) underTest.clusterUpgradeFinished();
        initAction(action);
        ReflectionTestUtils.setField(action, "stackImageService", stackImageService);

        new AbstractActionTestSupport<>(action).doExecute(context(), new ConfigureClusterManagerManagementServicesSuccess(STACK_ID), variables);

        verify(clusterUpgradeService).clusterUpgradeFinished(STACK_ID, properties.targetImage().packageVersions(), properties, true);
        verifyNoInteractions(resolver);
    }

    private ClusterUpgradeProperties properties() {
        return properties(false);
    }

    private ClusterUpgradeProperties properties(boolean prewarmed) {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2-build")
                .withParcel("https://runtime/redhat9/yum/").withCsd(List.of("runtime-csd"));
        ClouderaManagerProduct flink = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20")
                .withParcel("https://flink/redhat9/yum/").withCsd(List.of("flink-csd"));
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "base-image-version", OsType.RHEL9, "x86_64",
                cdh, Set.of(flink), new ClouderaManagerRepo().withVersion("7.13.2").withBaseUrl("https://cm/redhat9/yum/"));
        if (!prewarmed) {
            return properties;
        }
        ClusterUpgradeProperties.TargetImageUpgradeContext target = properties.targetImage();
        return new ClusterUpgradeProperties(properties.options(), properties.currentImage(), new ClusterUpgradeProperties.TargetImageUpgradeContext(
                target.imageId(), target.catalogName(), target.catalogUrl(), target.runtimeVersion(), "7.3.2", target.cdhBuildNumber(), target.packageVersions(),
                target.tags(), target.osType(), target.os(), target.architecture(), target.date(), target.created(), target.imageName(),
                new ImageStackDetails("7.3.2", null, "123"), target.repo(), target.preWarmParcelEntries(), target.preWarmCsd(), target.cdhParcel(),
                target.preWarmParcels(), target.clouderaManagerRepo()));
    }

    private Map<Object, Object> variables(ClusterUpgradeProperties properties) {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(FLOW_VARIABLE_NAME, properties);
        variables.put(AbstractClusterUpgradeAction.ROLLING_UPGRADE_ENABLED, true);
        return variables;
    }

    private ClusterUpgradeContext context() {
        return new ClusterUpgradeContext(mock(FlowParameters.class), new StackEvent(STACK_ID));
    }

    private void initAction(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, "clusterUpgradePropertiesResolver", resolver);
    }
}
