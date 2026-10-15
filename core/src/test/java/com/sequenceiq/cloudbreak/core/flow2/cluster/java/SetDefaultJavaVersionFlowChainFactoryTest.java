package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.chain.util.SetDefaultJavaVersionFlowChainService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event.ClusterServicesRestartTriggerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class SetDefaultJavaVersionFlowChainFactoryTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stack;

    @Mock
    private SetDefaultJavaVersionFlowChainService javaService;

    @Spy
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @InjectMocks
    private SetDefaultJavaVersionFlowChainFactory underTest;

    @Test
    void testCreateFlowTriggerEventQueueWithRestartCMAndRestartServicesAndRollingRestart() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", true, true, true);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        StackEvent thirdFlow = (StackEvent) queue.poll();
        assertEquals(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), thirdFlow.getSelector());
        ClusterServicesRestartTriggerEvent fourthFlow = (ClusterServicesRestartTriggerEvent) queue.poll();
        assertEquals(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), fourthFlow.getSelector());
        assertTrue(fourthFlow.isRollingRestart());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithRestartCMAndRestartServicesAndNoRollingRestart() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", true, true, false);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        StackEvent thirdFlow = (StackEvent) queue.poll();
        assertEquals(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), thirdFlow.getSelector());
        ClusterServicesRestartTriggerEvent fourthFlow = (ClusterServicesRestartTriggerEvent) queue.poll();
        assertEquals(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), fourthFlow.getSelector());
        assertFalse(fourthFlow.isRollingRestart());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithRestartCMAndNoRestartServices() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", false, true, false);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        assertEquals(3, queue.size());
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        StackEvent thirdFlow = (StackEvent) queue.poll();
        assertEquals(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), thirdFlow.getSelector());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithNoRestartCMAndNoRestartServices() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", false, false, false);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        assertEquals(2, queue.size());
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithNoRestartCMAndRestartServices() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", true, false, true);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        assertEquals(3, queue.size());
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        ClusterServicesRestartTriggerEvent thirdFlow = (ClusterServicesRestartTriggerEvent) queue.poll();
        assertEquals(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), thirdFlow.getSelector());
        assertTrue(thirdFlow.isRollingRestart());
    }

    @Test
    void missingUpgradePropertiesKeepCurrentJavaVersionAndDoNotBlockChain() {
        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(new SetDefaultJavaVersionForUpgradeTriggerEvent(1L), Map.of());

        assertThat(result.getQueue()).isEmpty();
        verifyNoInteractions(javaService, stackDtoService, clouderaManagerProductsProvider);
    }

    @ParameterizedTest
    @CsvSource({
            "false, , 7.3.1.500, 7.3.1.500",
            "false, 7.3.1-1.cdh7.3.1.p400.12345, 7.3.1.500, 7.3.1.500",
            "false, , , 7.3.1",
            "true, 7.3.1-1.cdh7.3.1.p500.12345, 7.3.1.400, 7.3.1.500"
    })
    void javaSelectionPreservesPrewarmedReleaseTagAndExplicitBaseImageVersion(boolean baseImage, String parcelVersion, String releaseTag,
            String expectedRuntime) throws Exception {
        Map<String, Object> targetData = new HashMap<>();
        targetData.put("runtimeVersion", "7.3.1");
        targetData.put("tags", releaseTag == null ? Map.of() : Map.of("release-version", releaseTag));
        if (parcelVersion != null) {
            targetData.put("cdhParcel", new ClouderaManagerProduct().withName("CDH").withVersion(parcelVersion));
        }
        if (!baseImage) {
            targetData.put("stackDetails", Map.of("version", "7.3.1", "repo", Map.of("stack", Map.of())));
        }
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.1");
        ClusterUpgradeProperties.TargetImageUpgradeContext target = JsonUtil.readValue(JsonUtil.writeValueAsString(targetData),
                ClusterUpgradeProperties.TargetImageUpgradeContext.class);
        properties = new ClusterUpgradeProperties(properties.options(), properties.currentImage(), target);
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stack);

        underTest.createFlowTriggerEventQueue(new SetDefaultJavaVersionForUpgradeTriggerEvent(1L),
                Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        verify(javaService).setDefaultJavaVersionTriggerEvent(stack, "7.2.17", expectedRuntime);
    }

    @Test
    void testUpgradeJavaChainUsesResolvedRuntime() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        SetDefaultJavaVersionForUpgradeTriggerEvent trigger = new SetDefaultJavaVersionForUpgradeTriggerEvent(1L);
        SetDefaultJavaVersionTriggerEvent javaEvent = new SetDefaultJavaVersionTriggerEvent(
                trigger.selector(), 1L, "17", false, false, false);
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stack);
        when(javaService.setDefaultJavaVersionTriggerEvent(stack, "7.2.17", "7.3.2")).thenReturn(List.of(javaEvent));

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(trigger, Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        assertThat(result.getQueue()).hasSize(2);
        SetDefaultJavaVersionTriggerEvent selected = (SetDefaultJavaVersionTriggerEvent) result.getQueue().remove();
        assertThat(selected.getDefaultJavaVersion()).isEqualTo("17");
        assertThat(selected.isRestartCM()).isFalse();
        assertThat(selected.isRestartServices()).isFalse();
        verify(javaService).setDefaultJavaVersionTriggerEvent(stack, "7.2.17", "7.3.2");
    }

    @Test
    void testBaseImageUpgradeUsesExplicitCdhParcelVersion() {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName("CDH")
                .withVersion("7.3.1-1.cdh7.3.1.p500.12345").withParcel("https://parcel-repo/");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.1", "base-image-version",
                OsType.RHEL9, "x86_64", cdh, Set.of(), null);
        SetDefaultJavaVersionForUpgradeTriggerEvent trigger = new SetDefaultJavaVersionForUpgradeTriggerEvent(1L);
        SetDefaultJavaVersionTriggerEvent javaEvent = new SetDefaultJavaVersionTriggerEvent(trigger.selector(), 1L, "17", false, false, false);
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stack);
        when(javaService.setDefaultJavaVersionTriggerEvent(stack, "7.2.17", "7.3.1.500")).thenReturn(List.of(javaEvent));

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(trigger, Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        assertThat(result.getQueue()).hasSize(2);
        assertThat(((SetDefaultJavaVersionTriggerEvent) result.getQueue().remove()).getDefaultJavaVersion()).isEqualTo("17");
        verify(javaService).setDefaultJavaVersionTriggerEvent(stack, "7.2.17", "7.3.1.500");
    }

    @Test
    void testUpgradeJavaChainUsesOriginalServicePackVersion() {
        ClouderaManagerProduct currentCdh = new ClouderaManagerProduct().withName("CDH")
                .withVersion("7.3.1-1.cdh7.3.1.p600.12345");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withCurrentProducts(
                ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.3.0"), Set.of(currentCdh), null);
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stack);

        underTest.createFlowTriggerEventQueue(new SetDefaultJavaVersionForUpgradeTriggerEvent(1L),
                Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        verify(javaService).setDefaultJavaVersionTriggerEvent(stack, "7.3.1.600", "7.3.3.0");
    }

    @Test
    void testMissingParcelAndRuntimeVersionsArePassedAsEmptyStrings() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withCurrentAndTargetRuntime(null, null, false, true, false);
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stack);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(new SetDefaultJavaVersionForUpgradeTriggerEvent(1L),
                Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        assertThat(result.getQueue()).isEmpty();
        verify(javaService).setDefaultJavaVersionTriggerEvent(stack, "", "");
    }

    @Test
    void testUpgradeJavaChainSkipsWhenJavaChangeIsNotNeeded() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        when(stackDtoService.getByIdWithoutResources(1L)).thenReturn(stack);

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(new SetDefaultJavaVersionForUpgradeTriggerEvent(1L),
                Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        assertThat(result.getQueue()).isEmpty();
        verify(javaService).setDefaultJavaVersionTriggerEvent(stack, "7.2.17", "7.3.2");
    }

    @Test
    void testStandaloneJavaUpgradePreservesRequestedVersionAndRestartOptions() {
        SetDefaultJavaVersionTriggerEvent trigger = new SetDefaultJavaVersionTriggerEvent("selector", 1L, "11", true, true, true);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");

        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(trigger, Map.of(ClusterUpgradeProperties.FLOW_VARIABLE_NAME, properties));

        assertThat(result.getQueue()).hasSize(4);
        SetDefaultJavaVersionTriggerEvent selected = (SetDefaultJavaVersionTriggerEvent) result.getQueue().remove();
        assertThat(selected.getDefaultJavaVersion()).isEqualTo("11");
        assertThat(selected.isRestartCM()).isTrue();
        assertThat(selected.isRestartServices()).isTrue();
        assertThat(selected.isRollingRestart()).isTrue();
        verifyNoInteractions(javaService, stackDtoService);
    }

    @Test
    void testDeferredUpgradeTriggerSurvivesSerialization() throws Exception {
        SetDefaultJavaVersionForUpgradeTriggerEvent trigger = new SetDefaultJavaVersionForUpgradeTriggerEvent(1L);

        SetDefaultJavaVersionTriggerEvent restored = JsonUtil.readValue(JsonUtil.writeValueAsString(trigger), SetDefaultJavaVersionTriggerEvent.class);

        assertThat(restored).isInstanceOf(SetDefaultJavaVersionForUpgradeTriggerEvent.class);
        assertThat(restored.getResourceId()).isEqualTo(1L);
        assertThat(restored.selector()).isEqualTo(trigger.selector());
    }
}
