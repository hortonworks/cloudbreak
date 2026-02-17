package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.chain.util.SetDefaultJavaVersionFlowChainService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.EmbeddedDbUpgradeFlowTriggersFactory;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ClusterSizeUpgradeValidator;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class UpgradeDistroxFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "imageId";

    private final ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID, "imageCatalogName", "imageCatUrl");

    @InjectMocks
    private UpgradeDistroxFlowEventChainFactory underTest;

    @Mock
    private ClusterRepairService clusterRepairService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private EmbeddedDbUpgradeFlowTriggersFactory embeddedDbUpgradeFlowTriggersFactory;

    @Mock
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterSizeUpgradeValidator clusterSizeUpgradeValidator;

    @Mock
    private SetDefaultJavaVersionFlowChainService setDefaultJavaVersionFlowChainService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Test
    void testInitEvent() {
        assertEquals(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testChainQueueForNonReplaceVms() throws CloudbreakImageNotFoundException {
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(Image.builder().withOsType(OsType.RHEL9.getOsType()).build());
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        when(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(1L))
                .thenReturn(List.of(new SecretRotationFlowChainTriggerEvent(null, 1L, null, List.of(SALT_MASTER_KEY_PAIR), null, null)));
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, false, false, "variant", true, "runtime");
        SetDefaultJavaVersionTriggerEvent setDefaultJavaEvent =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), STACK_ID,
                        "17", false, false, false);
        when(setDefaultJavaVersionFlowChainService.setDefaultJavaVersionTriggerEvent(eq(stackDto), eq(imageChangeDto)))
                .thenReturn(List.of(setDefaultJavaEvent));
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(9, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertUpdatePreparationEvent(flowChainQueue, IMAGE_ID);
        assertSaltSecretRotationTriggerEvent(flowChainQueue);
        assertSaltUpdateEvent(flowChainQueue);
        assertSetDefaultJavaEvent(flowChainQueue);
        assertUpgradeEvent(flowChainQueue, IMAGE_ID);
        assertImageUpdateEvent(flowChainQueue);
    }

    @Test
    void testCreateFlowTriggerEventQueueShouldThrowExceptionWhenImageNotFound() throws CloudbreakImageNotFoundException {
        doThrow(new CloudbreakImageNotFoundException("error")).when(componentConfigProviderService).getImage(STACK_ID);
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        when(saltVersionUpgradeService.getSaltSecretRotationTriggerEvent(1L))
                .thenReturn(List.of(new SecretRotationFlowChainTriggerEvent(null, 1L, null, List.of(SALT_MASTER_KEY_PAIR), null, null)));
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, false, false, "variant", true, "runtime");
        SetDefaultJavaVersionTriggerEvent setDefaultJavaEvent =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), STACK_ID,
                        "17", false, false, false);
        when(setDefaultJavaVersionFlowChainService.setDefaultJavaVersionTriggerEvent(eq(stackDto), eq(imageChangeDto)))
                .thenReturn(List.of(setDefaultJavaEvent));

        String errorMessage = Assertions.assertThrows(NotFoundException.class, () -> underTest.createFlowTriggerEventQueue(event)).getMessage();

        assertEquals("Image not found for stack", errorMessage);
    }

    @Test
    void testChainQueueForUpgradeWithStoppedNodes() throws CloudbreakImageNotFoundException {
        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        host1.setClusterManagerServer(true);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(host1, host2));
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(Image.builder().withOsType(OsType.RHEL9.getOsType()).build());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, false, false, "variant", true, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(8, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertUpdatePreparationEvent(flowChainQueue, IMAGE_ID);
        assertClusterScaleTriggerEvent(flowChainQueue);
        assertSaltUpdateEvent(flowChainQueue);
        assertUpgradeEvent(flowChainQueue, IMAGE_ID);
        assertImageUpdateEvent(flowChainQueue);
    }

    private void assertClusterSyncTriggerEvent(FlowTriggerEventQueue flowTriggerEventQueue) {
        Selectable event = flowTriggerEventQueue.getQueue().remove();
        assertEquals(ClusterSyncEvent.CLUSTER_SYNC_EVENT.event(), event.getSelector());
        assertEquals(STACK_ID, event.getResourceId());
        assertInstanceOf(StackEvent.class, event);
    }

    private void assertSyncTriggerEvent(FlowTriggerEventQueue flowTriggerEventQueue) {
        Selectable event = flowTriggerEventQueue.getQueue().remove();
        assertEquals(STACK_SYNC_EVENT.event(), event.getSelector());
        assertEquals(STACK_ID, event.getResourceId());
        assertInstanceOf(StackSyncTriggerEvent.class, event);
    }

    @Test
    void testChainQueueForRollingUpgradeWithReplaceVms() {
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        lenient().when(stackDto.getPlatformVariant()).thenReturn("originalVariant");
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);

        Set<InstanceMetaData> instances = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setDiscoveryFQDN("master-" + i);
            instances.add(instanceMetaData);
        }
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(Map.of(HostGroupName.hostGroupName("master"), instances));
        when(clusterRepairService.validateRepair(any(), anyLong(), any(), eq(false))).thenReturn(repairStartResult);

        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, true, true, "variant", true, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(6, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertSaltUpdateEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
        assertRepairEvent(flowChainQueue, RepairType.ONE_BY_ONE, 2, "variant");
    }

    @Test
    void testChainQueueForReplaceVmsWithHundredNodes() {
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        lenient().when(stackDto.getPlatformVariant()).thenReturn("originalVariant");
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        Set<InstanceMetaData> instances = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setDiscoveryFQDN("compute-" + i);
            instances.add(instanceMetaData);
        }
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(Map.of(HostGroupName.hostGroupName("compute"), instances));
        when(clusterRepairService.validateRepair(any(), anyLong(), any(), eq(false))).thenReturn(repairStartResult);

        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, true, true, "variant", false, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(6, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertSaltUpdateEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
        assertRepairEvent(flowChainQueue, RepairType.BATCH, 500, "variant");
        verify(clusterRepairService, times(1)).validateRepair(eq(ManualClusterRepairMode.ALL), eq(STACK_ID), eq(Set.of()), eq(false));
    }

    @Test
    void testChainQueueForReplaceVmsWithHundredNodesWhenForceOsUpgradeAndRollingUpgradeEnabled() throws CloudbreakImageNotFoundException {
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(Image.builder().withOsType(OsType.RHEL9.getOsType()).build());
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        lenient().when(stackDto.getPlatformVariant()).thenReturn("originalVariant");
        InstanceMetadataView master1 = mock(InstanceMetadataView.class);
        when(master1.getInstanceId()).thenReturn("master-1");
        InstanceMetadataView master2 = mock(InstanceMetadataView.class);
        when(master2.getInstanceId()).thenReturn("master-2");
        when(stackDto.getAllAvailableGatewayInstances()).thenReturn(List.of(master1, master2));
        when(clusterSizeUpgradeValidator.isClusterSizeLargerThanAllowedForRollingUpgrade(anyLong())).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);
        Set<InstanceMetaData> instances = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setDiscoveryFQDN("master-" + i);
            instances.add(instanceMetaData);
        }
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(Map.of(HostGroupName.hostGroupName("master"), instances));
        when(clusterRepairService.validateRepair(any(), anyLong(), any(), eq(false))).thenReturn(repairStartResult);

        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, true, false, "variant", true, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(8, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertUpdatePreparationEvent(flowChainQueue, IMAGE_ID);
        assertSaltUpdateEvent(flowChainQueue);
        assertUpgradeEvent(flowChainQueue, IMAGE_ID);
        assertImageUpdateEvent(flowChainQueue);
        assertRepairEvent(flowChainQueue, RepairType.ONE_BY_ONE, 2, "originalVariant");
        verify(clusterRepairService, times(1)).validateRepair(eq(ManualClusterRepairMode.NODE_ID), eq(STACK_ID), eq(Set.of("master-1", "master-2")), eq(false));
    }

    @Test
    void testChainQueueForOsUpgradeShouldFilterOutAlreadyUpgradedInstances() {
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        lenient().when(stackDto.getPlatformVariant()).thenReturn("originalVariant");
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);

        Set<InstanceMetaData> instances = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN("master-1");
        instanceMetaData1.setImage(new Json(com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId(IMAGE_ID).build()));
        instances.add(instanceMetaData1);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setDiscoveryFQDN("master-2");
        instanceMetaData2.setImage(new Json(com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId("OLD_IMAGE").build()));
        instances.add(instanceMetaData2);

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(Map.of(HostGroupName.hostGroupName("master"), instances));
        when(clusterRepairService.validateRepair(any(), anyLong(), any(), eq(false))).thenReturn(repairStartResult);

        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, true, true, "variant", false, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(6, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertSaltUpdateEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
        assertRepairEvent(flowChainQueue, RepairType.ALL_AT_ONCE, 1, "variant");
    }

    @Test
    void testChainQueueForOsUpgradeShouldSkipOsUpgradeWhenNoUpgradableInstanceFound() {
        when(stackDtoService.getByIdWithoutResources(STACK_ID)).thenReturn(stackDto);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);

        Set<InstanceMetaData> instances = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN("master-1");
        instanceMetaData1.setImage(new Json(com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId(IMAGE_ID).build()));
        instances.add(instanceMetaData1);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setDiscoveryFQDN("master-2");
        instanceMetaData2.setImage(new Json(com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId(IMAGE_ID).build()));
        instances.add(instanceMetaData2);

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(Map.of(HostGroupName.hostGroupName("master"), instances));
        when(clusterRepairService.validateRepair(any(), anyLong(), any(), eq(false))).thenReturn(repairStartResult);

        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, true, true, "variant", false, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(5, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertSaltUpdateEvent(flowChainQueue);
        assertImageUpdateEvent(flowChainQueue);
    }

    @Test
    void testChainQueueForOsUpgradeWhenReplaceVmsIsFalse() {
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of());
        ReflectionTestUtils.setField(underTest, "batchRepairEnabled", true);

        DistroXUpgradeFlowChainTriggerEvent event = new DistroXUpgradeFlowChainTriggerEvent(FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT,
                STACK_ID, new Promise<>(), imageChangeDto, false, true, "variant", false, "runtime");
        FlowTriggerEventQueue flowChainQueue = underTest.createFlowTriggerEventQueue(event);
        assertEquals(4, flowChainQueue.getQueue().size());
        assertSyncTriggerEvent(flowChainQueue);
        assertClusterSyncTriggerEvent(flowChainQueue);
        assertUpdateValidationEvent(flowChainQueue, IMAGE_ID, event.isReplaceVms(), event.isLockComponents(), event.isRollingUpgradeEnabled());
        assertSaltUpdateEvent(flowChainQueue);
    }

    private void assertUpdateValidationEvent(FlowTriggerEventQueue flowChainQueue, String imageId, boolean replaceVms, boolean lockComponents,
            boolean rollingUpgradeEnabled) {
        Selectable upgradeValidationEvent = flowChainQueue.getQueue().remove();
        assertEquals(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), upgradeValidationEvent.selector());
        assertEquals(STACK_ID, upgradeValidationEvent.getResourceId());
        assertInstanceOf(ClusterUpgradeValidationTriggerEvent.class, upgradeValidationEvent);
        ClusterUpgradeValidationTriggerEvent validationEvent = (ClusterUpgradeValidationTriggerEvent) upgradeValidationEvent;
        assertEquals(imageId, validationEvent.getImageId());
        assertEquals(lockComponents, validationEvent.isLockComponents());
        assertEquals(replaceVms, validationEvent.isReplaceVms());
        assertEquals(rollingUpgradeEnabled, validationEvent.isRollingUpgradeEnabled());
    }

    private void assertUpdatePreparationEvent(FlowTriggerEventQueue flowChainQueue, String imageId) {
        Selectable upgradePreparationEvent = flowChainQueue.getQueue().remove();
        assertEquals(START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT.event(), upgradePreparationEvent.selector());
        assertEquals(STACK_ID, upgradePreparationEvent.getResourceId());
        assertInstanceOf(ClusterUpgradePreparationTriggerEvent.class, upgradePreparationEvent);
        assertEquals(imageId, ((ClusterUpgradePreparationTriggerEvent) upgradePreparationEvent).getImageChangeDto().getImageId());
    }

    private void assertClusterScaleTriggerEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable clusterScaleEvent = flowChainQueue.getQueue().remove();
        assertEquals(STOPSTART_UPSCALE_TRIGGER_EVENT.event(), clusterScaleEvent.selector());
        assertEquals(STACK_ID, clusterScaleEvent.getResourceId());
        assertInstanceOf(StopStartUpscaleTriggerEvent.class, clusterScaleEvent);
    }

    private void assertSaltSecretRotationTriggerEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable saltSecretRotationEvent = flowChainQueue.getQueue().remove();
        assertInstanceOf(SecretRotationFlowChainTriggerEvent.class, saltSecretRotationEvent);
        SecretRotationFlowChainTriggerEvent secretRotationFlowChainTriggerEvent = (SecretRotationFlowChainTriggerEvent) saltSecretRotationEvent;
        assertThat(secretRotationFlowChainTriggerEvent.getSecretTypes()).containsExactlyInAnyOrder(SALT_MASTER_KEY_PAIR);
    }

    private void assertSaltUpdateEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable saltUpdateEvent = flowChainQueue.getQueue().remove();
        assertEquals(SALT_UPDATE_EVENT.event(), saltUpdateEvent.selector());
        assertEquals(STACK_ID, saltUpdateEvent.getResourceId());
        assertInstanceOf(StackEvent.class, saltUpdateEvent);
    }

    private void assertUpgradeEvent(FlowTriggerEventQueue flowChainQueue, String imageId) {
        Selectable upgradeEvent = flowChainQueue.getQueue().remove();
        assertEquals(CLUSTER_UPGRADE_INIT_EVENT.event(), upgradeEvent.selector());
        assertEquals(STACK_ID, upgradeEvent.getResourceId());
        assertInstanceOf(ClusterUpgradeTriggerEvent.class, upgradeEvent);
        assertEquals(imageId, ((ClusterUpgradeTriggerEvent) upgradeEvent).getImageId());
    }

    private void assertImageUpdateEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable imageUpdateEvent = flowChainQueue.getQueue().remove();
        assertEquals(STACK_IMAGE_UPDATE_TRIGGER_EVENT, imageUpdateEvent.selector());
        assertEquals(STACK_ID, imageUpdateEvent.getResourceId());
        assertInstanceOf(StackImageUpdateTriggerEvent.class, imageUpdateEvent);
        StackImageUpdateTriggerEvent event = (StackImageUpdateTriggerEvent) imageUpdateEvent;
        assertEquals(imageChangeDto.getImageId(), event.getNewImageId());
        assertEquals(imageChangeDto.getImageCatalogName(), event.getImageCatalogName());
        assertEquals(imageChangeDto.getImageCatalogUrl(), event.getImageCatalogUrl());
    }

    private void assertRepairEvent(FlowTriggerEventQueue flowChainQueue, RepairType repairType, int nodeCount, String expectedTriggerStackVariant) {
        Selectable repairEvent = flowChainQueue.getQueue().remove();
        ClusterRepairTriggerEvent clusterRepairTriggerEvent = (ClusterRepairTriggerEvent) repairEvent;
        assertEquals(CLUSTER_REPAIR_TRIGGER_EVENT, repairEvent.selector());
        assertEquals(repairType, clusterRepairTriggerEvent.getRepairType());
        assertEquals(STACK_ID, repairEvent.getResourceId());
        assertInstanceOf(ClusterRepairTriggerEvent.class, repairEvent);
        assertEquals(nodeCount, clusterRepairTriggerEvent.getFailedNodesMap().entrySet().iterator().next().getValue().size());
        assertEquals(expectedTriggerStackVariant, clusterRepairTriggerEvent.getTriggeredStackVariant());
    }

    private void assertSetDefaultJavaEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable javaEvent = flowChainQueue.getQueue().remove();
        assertInstanceOf(SetDefaultJavaVersionTriggerEvent.class, javaEvent);
    }

    private InstanceMetaData getHost(String hostName, String groupName, InstanceStatus instanceStatus, InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setGroupName(groupName);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceMetaData.setInstanceId(hostName);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));

        return instanceMetaData;
    }
}