package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DiskValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ImageValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.EmbeddedDbUpgradeFlowTriggersFactory;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RescheduleStatusCheckTriggerEvent;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class ClusterRepairFlowEventChainFactoryTest {

    private static final long INSTANCE_GROUP_ID = 1L;

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final String HG_MASTER = "hostGroup-master";

    private static final String HG_CORE = "hostGroup-core";

    private static final String HG_AUXILIARY = "hostGroup-auxiliary";

    private static final String FAILED_PRIMARY_GATEWAY_FQDN = "failedNode-FQDN-primary-gateway";

    private static final String FAILED_SECONDARY_GATEWAY_FQDN_1 = "failedNode-FQDN-secondary-gateway-1";

    private static final String FAILED_SECONDARY_GATEWAY_FQDN_2 = "failedNode-FQDN-secondary-gateway-2";

    private static final String FAILED_CORE_FQDN = "failedNode-FQDN-core";

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackUpgradeService stackUpgradeService;

    @Mock
    private StackDto stackDto;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private DefaultRootVolumeSizeProvider rootVolumeSizeProvider;

    @Mock
    private SkuMigrationService skuMigrationService;

    @InjectMocks
    private ClusterRepairFlowEventChainFactory underTest;

    @Mock
    private EmbeddedDbUpgradeFlowTriggersFactory embeddedDbUpgradeFlowTriggersFactory;

    @BeforeEach
    void setup() {
        setupStackDto();
        setupViews();
    }

    @Test
    void testRepairSingleGatewayWithNoAttached() {
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "SingleGatewayWhenItsFailed");
    }

    @Test
    void testRepairSingleGatewayWithNoAttachedWithEmbeddedDBUpgrade() {
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.GATEWAY);
        when(embeddedDbUpgradeFlowTriggersFactory.createFlowTriggers(stackDto, true))
                .thenReturn(List.of(new StackEvent("UPGRADE_EMBEDDEDDB_PREPARATION_TRIGGER_EVENT", STACK_ID),
                        new StackEvent("UPGRADE_RDS_TRIGGER_EVENT", STACK_ID)));

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withUpgrade().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "UPGRADE_EMBEDDEDDB_PREPARATION_TRIGGER_EVENT",
                "UPGRADE_RDS_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "SingleGatewayWhenItsFailedWithDBUpgrade");
    }

    @Test
    void testRepairSingleGatewayWithAttached() {
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    void testRepairSingleGatewayMultipleNodes() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    void testRepairMultipleGatewayWithNoAttached() {
        Stack stack = getStack();
        setupInstanceGroup(InstanceGroupType.GATEWAY, 5);
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    void testRepairMultipleGatewayWithAttached() {
        Stack stack = getStack();
        setupInstanceGroup(InstanceGroupType.GATEWAY, 5);
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "MultipleGateway");
    }

    @Test
    void testRepairCoreNodes() {
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.CORE);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    void testRepairCoreNodesWithStoppedNodes() {
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.CORE);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        host1.setClusterManagerServer(true);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        HostGroup hostGroup2 = new HostGroup();
        hostGroup2.setName("hostGroup2");
        hostGroup2.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host2 = getHost("host2", hostGroup2.getName(), InstanceStatus.STOPPED, InstanceGroupType.CORE);
        hostGroup2.setInstanceGroup(host2.getInstanceGroup());

        when(stackDto.getNotTerminatedInstanceMetaData()).thenReturn(List.of(host1, host2));
        when(stackDto.getStack().getType()).thenReturn(StackType.WORKLOAD);
        lenient().when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STOPSTART_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "CoreNodesWithStoppedNodes");
    }

    @Test
    void testRepairNotGatewayInstanceGroup() {
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.CORE);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "OnNotGatewayInstanceGroup");
    }

    @Test
    void testRepairAllAtOnce() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupHostGroup(HG_AUXILIARY, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack)
                .withFailedPrimaryGateway()
                .withFailedSecondaryGateway()
                .with3FailedCore()
                .withFailedAuxiliary()
                .withRepairType(RepairType.ALL_AT_ONCE)
                .build();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        assertThat(eventQueues.getQueue(), hasSize(7));
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedDeque<>(eventQueues.getQueue());
        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainInitPayload);

        ImageValidationTriggerEvent imageValidationTriggerEvent = (ImageValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(imageValidationTriggerEvent);
        DiskValidationTriggerEvent diskValidationTriggerEvent = (DiskValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(diskValidationTriggerEvent);

        StackDownscaleTriggerEvent downscale = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscale, HG_MASTER, Set.of(FAILED_PRIMARY_GATEWAY_FQDN, FAILED_SECONDARY_GATEWAY_FQDN_1));
        assertGroupWithHosts(downscale, HG_CORE, Set.of("core1", "core2", "core3"));
        assertGroupWithHosts(downscale, HG_AUXILIARY, Set.of("aux1"));

        StackAndClusterUpscaleTriggerEvent upscale = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscale, HG_MASTER, Set.of(FAILED_PRIMARY_GATEWAY_FQDN, FAILED_SECONDARY_GATEWAY_FQDN_1));
        assertGroupWithHosts(upscale, HG_CORE, Set.of("core1", "core2", "core3"));
        assertGroupWithHosts(upscale, HG_AUXILIARY, Set.of("aux1"));

        RescheduleStatusCheckTriggerEvent statusCheckTriggerEvent = (RescheduleStatusCheckTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(statusCheckTriggerEvent);

        FlowChainFinalizePayload flowChainFinalizePayload = (FlowChainFinalizePayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainFinalizePayload);

        eventQueues.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "RepairAllAtOnce");
    }

    @Test
    void testRepairNodesOneByOne() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupHostGroup(HG_AUXILIARY, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN(FAILED_PRIMARY_GATEWAY_FQDN);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack)
                .withFailedPrimaryGateway()
                .withFailedSecondaryGateway()
                .with3FailedCore()
                .withFailedAuxiliary()
                .withRepairType(RepairType.ONE_BY_ONE)
                .build();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT", "IMAGE_VALIDATION_EVENT", "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        assertThat(eventQueues.getQueue(), hasSize(17));
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedDeque<>(eventQueues.getQueue());
        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainInitPayload);

        ImageValidationTriggerEvent imageValidationTriggerEvent = (ImageValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(imageValidationTriggerEvent);
        DiskValidationTriggerEvent diskValidationTriggerEvent = (DiskValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(diskValidationTriggerEvent);

        StackDownscaleTriggerEvent downscaleMaster1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscaleMaster1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);
        StackAndClusterUpscaleTriggerEvent upscaleMaster1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscaleMaster1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);

        ClusterAndStackDownscaleTriggerEvent downscaleMaster2 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscaleMaster2, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN_1);
        StackAndClusterUpscaleTriggerEvent upscaleMaster2 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscaleMaster2, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN_1);

        ClusterAndStackDownscaleTriggerEvent downscale5 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale5, HG_AUXILIARY, "aux1");
        StackAndClusterUpscaleTriggerEvent upscale5 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale5, HG_AUXILIARY, "aux1");

        ClusterAndStackDownscaleTriggerEvent downscale2 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale2, HG_CORE, "core1");
        StackAndClusterUpscaleTriggerEvent upscale2 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale2, HG_CORE, "core1");

        ClusterAndStackDownscaleTriggerEvent downscale3 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale3, HG_CORE, "core2");
        StackAndClusterUpscaleTriggerEvent upscale3 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale3, HG_CORE, "core2");

        ClusterAndStackDownscaleTriggerEvent downscale4 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale4, HG_CORE, "core3");
        StackAndClusterUpscaleTriggerEvent upscale4 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale4, HG_CORE, "core3");

        RescheduleStatusCheckTriggerEvent statusCheckTriggerEvent = (RescheduleStatusCheckTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(statusCheckTriggerEvent);

        FlowChainFinalizePayload flowChainFinalizePayload = (FlowChainFinalizePayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainFinalizePayload);

        eventQueues.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "OneByOne");
    }

    @Test
    void testBatchRepair() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupHostGroup(HG_AUXILIARY, setupInstanceGroup(InstanceGroupType.CORE));
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        setupPrimaryGateway();

        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack)
                .withFailedPrimaryGateway()
                .withFailedSecondaryGateways()
                .with320FailedCore()
                .withFailedAuxiliary()
                .withRepairType(RepairType.BATCH)
                .build();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT", "IMAGE_VALIDATION_EVENT", "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT", "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedDeque<>(eventQueues.getQueue());
        eventQueues.getQueue().remove();

        ImageValidationTriggerEvent imageValidationTriggerEvent = (ImageValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(imageValidationTriggerEvent);
        DiskValidationTriggerEvent diskValidationTriggerEvent = (DiskValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(diskValidationTriggerEvent);

        StackDownscaleTriggerEvent downscale1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscale1, HG_MASTER, Set.of(FAILED_PRIMARY_GATEWAY_FQDN, FAILED_SECONDARY_GATEWAY_FQDN_1, FAILED_SECONDARY_GATEWAY_FQDN_2));
        assertGroupWithHosts(downscale1, HG_CORE, hosts("core-", 0, 96));
        assertGroupWithHost(downscale1, HG_AUXILIARY, "aux1");

        StackAndClusterUpscaleTriggerEvent upscale1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscale1, HG_MASTER, Set.of(FAILED_PRIMARY_GATEWAY_FQDN, FAILED_SECONDARY_GATEWAY_FQDN_1, FAILED_SECONDARY_GATEWAY_FQDN_2));
        assertGroupWithHosts(upscale1, HG_CORE, hosts("core-", 0, 96));
        assertGroupWithHost(upscale1, HG_AUXILIARY, "aux1");

        ClusterAndStackDownscaleTriggerEvent downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscaleEvent, HG_CORE, hosts("core-", 96, 196));
        StackAndClusterUpscaleTriggerEvent upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscaleEvent, HG_CORE, hosts("core-", 96, 196));

        downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscaleEvent, HG_CORE, hosts("core-", 196, 296));
        upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscaleEvent, HG_CORE, hosts("core-", 196, 296));

        downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscaleEvent, HG_CORE, hosts("core-", 296, 320));

        upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscaleEvent, HG_CORE, hosts("core-", 296, 320));

        eventQueues.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "BatchRepair");
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenNotUpgrade() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(stackDto.getId(), Map.of(), RepairType.ALL_AT_ONCE, false, "variant");

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenUpgradeButNotAwsNativeVariantIsTheTriggered() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "triggeredVariant";
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackDto.getId(), RepairType.ALL_AT_ONCE, Map.of(),
                false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTheTriggeredButStackIsAlreadyOnAwsNativeVariant() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackDto.getId(), RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTheTriggeredOnTheLegacyAwsVariantAndNotEntitledForMigration() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackDto.getId(), RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTriggeredOnTheLegacyAwsVariantAndEntitledForMigration() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "AWS_NATIVE";
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(true);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackDto.getId(), RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertFalse(flowTriggers.isEmpty());
        AwsVariantMigrationTriggerEvent actual = (AwsVariantMigrationTriggerEvent) flowTriggers.peek();
        assertEquals(groupName, actual.getHostGroupName());
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenRepairForAllNodesAndAwsNativeVariantIsTriggeredOnTheLegacyAwsVariantAndEntitledForMigration() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "AWS_NATIVE";
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(true);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(true);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(stackDto.getId(), Map.of(), RepairType.ALL_AT_ONCE, false,
                triggeredVariant, false, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertFalse(flowTriggers.isEmpty());
        AwsVariantMigrationTriggerEvent actual = (AwsVariantMigrationTriggerEvent) flowTriggers.peek();
        assertEquals(groupName, actual.getHostGroupName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS"})
    @NullAndEmptySource
    void testAddAwsNativeMigrationIfNeedWhenRepairForAllNodesAndNotAwsNativeVariantIsTriggeredOnTheLegacyAwsVariantAndEntitledForMigration(
            String triggeredVariant) {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(true);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(false);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(stackDto.getId(), Map.of(), RepairType.ALL_AT_ONCE, false,
                triggeredVariant, false, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    void testAddAwsNativeMigrationIfNeedWhenRepairAndAwsNativeVariantIsTriggeredOnTheLegacyAwsVariantAndEntitledForMigrationButNotAllNodesRequestedInRepair() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "AWS";
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(false);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(stackDto.getId(), Map.of(), RepairType.ALL_AT_ONCE, false,
                triggeredVariant, false, false);


        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackDto);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    void testRootDiskMigration() {
        ReflectionTestUtils.setField(underTest, "rootDiskRepairMigrationEnabled", true);
        when(rootVolumeSizeProvider.getDefaultRootVolumeForPlatform(any(), eq(false))).thenReturn(200);
        Stack stack = getStack();
        setupHostGroup(InstanceGroupType.CORE);
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        com.sequenceiq.cloudbreak.view.InstanceGroupView instanceGroupView = mock(com.sequenceiq.cloudbreak.view.InstanceGroupView.class);
        when(instanceGroupView.getGroupName()).thenReturn("core");
        Template template = mock(Template.class);
        when(template.getRootVolumeSize()).thenReturn(100);
        when(template.getInstanceType()).thenReturn("instance");
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto));

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_VERTICAL_SCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        eventQueues.getQueue().remove();
        ImageValidationTriggerEvent imageValidationTriggerEvent = (ImageValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(imageValidationTriggerEvent);
        DiskValidationTriggerEvent diskValidationTriggerEvent = (DiskValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(diskValidationTriggerEvent);
        CoreVerticalScalingTriggerEvent coreVerticalScalingTriggerEvent = (CoreVerticalScalingTriggerEvent) eventQueues.getQueue().poll();
        assertEquals(200, coreVerticalScalingTriggerEvent.getRequest().getTemplate().getRootVolume().getSize().intValue());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "RootDiskMigration");
    }

    @Test
    void testSkuMigration() {
        when(skuMigrationService.isMigrationNecessary(any())).thenReturn(true);
        when(skuMigrationService.isRepairSkuMigrationEnabled()).thenReturn(true);
        Stack stack = getStack();
        stack.setId(STACK_ID);
        setupHostGroup(InstanceGroupType.CORE);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "SKU_MIGRATION_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        eventQueues.getQueue().remove();
        ImageValidationTriggerEvent imageValidationTriggerEvent = (ImageValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(imageValidationTriggerEvent);
        DiskValidationTriggerEvent diskValidationTriggerEvent = (DiskValidationTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(diskValidationTriggerEvent);
        SkuMigrationTriggerEvent skuMigrationTriggerEvent = (SkuMigrationTriggerEvent) eventQueues.getQueue().poll();
        assertEquals(STACK_ID, skuMigrationTriggerEvent.getResourceId());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "SkuMigration");
    }

    @Test
    void testRepairSingleGatewayWhenNonRollingUpgrade() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().withUpgrade().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "SingleGatewayWithoutRollingUpgrade");
    }

    @Test
    void testRepairSingleGatewayWhenRollingUpgrade() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().withUpgrade().withRollingRestartEnabled().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueues, "SingleGateway");
    }

    @Test
    void testRepairSingleGatewayWhenNotUpgrade() {
        Stack stack = getStack();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "IMAGE_VALIDATION_EVENT",
                "DISK_VALIDATION_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    private void assertGroupWithHost(StackScaleTriggerEvent scaleTriggerEvent, String group, String expectedInstanceFqdn) {
        assertGroupWithHosts(scaleTriggerEvent, group, Set.of(expectedInstanceFqdn));
    }

    private void assertGroupWithHost(ClusterScaleTriggerEvent scaleTriggerEvent, String group, String expectedInstanceFqdn) {
        assertGroupWithHosts(scaleTriggerEvent, group, Set.of(expectedInstanceFqdn));
    }

    private void assertGroupWithHosts(StackScaleTriggerEvent scaleTriggerEvent, String group, Set<String> expectedHosts) {
        Set<String> hosts = scaleTriggerEvent.getHostGroupsWithHostNames().get(group);
        assertEquals(expectedHosts, hosts);
    }

    private void assertGroupWithHosts(ClusterScaleTriggerEvent scaleTriggerEvent, String group, Set<String> expectedHosts) {
        Set<String> hosts = scaleTriggerEvent.getHostGroupsWithHostNames().get(group);
        assertEquals(expectedHosts, hosts);
    }

    private Set<String> hosts(String prefix, int startNumber, int endNumber) {
        Set<String> hosts = new HashSet<>();
        for (int i = startNumber; i < endNumber; i++) {
            hosts.add(prefix + i);
        }
        return hosts;
    }

    private void setupHostGroup(InstanceGroupType instanceGroupType) {
        String hostGroupName = instanceGroupType.name().toLowerCase();
        HostGroup hostGroup = setupHostGroup(hostGroupName, setupInstanceGroup(instanceGroupType));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));
        setupPrimaryGateway();
    }

    private void setupPrimaryGateway() {
        lenient().when(instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(anyLong(), anyLong()))
                .thenReturn(Optional.of(FAILED_PRIMARY_GATEWAY_FQDN));
    }

    private Stack getStack() {
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);
        return stack;
    }

    private void setupViews() {
        lenient().when(clusterView.getId()).thenReturn(CLUSTER_ID);
    }

    private void setupStackDto() {
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getStack()).thenReturn(stackView);
        lenient().when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        lenient().when(stackDto.getPrimaryGatewayFQDN()).thenReturn(Optional.of(FAILED_PRIMARY_GATEWAY_FQDN));
        lenient().when(stackDto.getSecondaryGatewayFQDNs()).thenReturn(Set.of(FAILED_SECONDARY_GATEWAY_FQDN_1, FAILED_SECONDARY_GATEWAY_FQDN_2));
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
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));

        return instanceMetaData;
    }

    private HostGroup setupHostGroup(String hostGroupName, InstanceGroup instanceGroup) {
        HostGroup hostGroup = mock(HostGroup.class);
        when(hostGroup.getName()).thenReturn(hostGroupName);
        when(hostGroup.getInstanceGroup()).thenReturn(instanceGroup);
        lenient().when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq(hostGroupName))).thenReturn(Optional.of(hostGroup));
        return hostGroup;
    }

    private InstanceGroup setupInstanceGroup(InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        lenient().when(instanceGroup.getId()).thenReturn(INSTANCE_GROUP_ID);
        when(instanceGroup.getInstanceGroupType()).thenReturn(instanceGroupType);
        return instanceGroup;
    }

    private void setupInstanceGroup(InstanceGroupType instanceGroupType, int nodeCount) {
        InstanceGroupView ig = mock(InstanceGroupView.class);
        lenient().when(ig.getInstanceGroupType()).thenReturn(instanceGroupType);
        when(ig.getNodeCount()).thenReturn(nodeCount);
        when(instanceGroupService.findViewByStackId(STACK_ID)).thenReturn(Set.of(ig));
    }

    private void assertEvents(FlowTriggerEventQueue eventQueues, List<String> expectedEvents) {
        assertEquals(expectedEvents, eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList()));
    }

    private static class TriggerEventBuilder {

        private final Stack stack;

        private final List<String> failedGatewayNodes = new ArrayList<>();

        private final List<String> failedCoreNodes = new ArrayList<>();

        private final List<String> failedAuxiliaryNodes = new ArrayList<>();

        private RepairType repairType = RepairType.ALL_AT_ONCE;

        private boolean upgrade;

        private boolean rollingRestartEnabled;

        private TriggerEventBuilder(Stack stack) {
            this.stack = stack;
        }

        private TriggerEventBuilder withFailedPrimaryGateway() {
            failedGatewayNodes.add(FAILED_PRIMARY_GATEWAY_FQDN);
            return this;
        }

        private TriggerEventBuilder withFailedSecondaryGateway() {
            failedGatewayNodes.add(FAILED_SECONDARY_GATEWAY_FQDN_1);
            return this;
        }

        private TriggerEventBuilder withFailedSecondaryGateways() {
            failedGatewayNodes.add(FAILED_SECONDARY_GATEWAY_FQDN_1);
            failedGatewayNodes.add(FAILED_SECONDARY_GATEWAY_FQDN_2);
            return this;
        }

        private TriggerEventBuilder withFailedCore() {
            failedCoreNodes.add(FAILED_CORE_FQDN);
            return this;
        }

        private TriggerEventBuilder with3FailedCore() {
            failedCoreNodes.add("core1");
            failedCoreNodes.add("core2");
            failedCoreNodes.add("core3");
            return this;
        }

        private TriggerEventBuilder with320FailedCore() {
            for (int i = 0; i < 320; i++) {
                failedCoreNodes.add("core-" + i);
            }
            return this;
        }

        private TriggerEventBuilder withFailedAuxiliary() {
            failedAuxiliaryNodes.add("aux1");
            return this;
        }

        private TriggerEventBuilder withRepairType(RepairType repairType) {
            this.repairType = repairType;
            return this;
        }

        private TriggerEventBuilder withUpgrade() {
            upgrade = true;
            return this;
        }

        private TriggerEventBuilder withRollingRestartEnabled() {
            rollingRestartEnabled = true;
            return this;
        }

        private ClusterRepairTriggerEvent build() {
            Map<String, List<String>> failedNodes = new LinkedHashMap<>();
            if (!failedGatewayNodes.isEmpty()) {
                failedNodes.put(HG_MASTER, failedGatewayNodes);
            }
            if (!failedCoreNodes.isEmpty()) {
                failedNodes.put(HG_CORE, failedCoreNodes);
            }
            if (!failedAuxiliaryNodes.isEmpty()) {
                failedNodes.put(HG_AUXILIARY, failedAuxiliaryNodes);
            }

            return new ClusterRepairTriggerEvent(stack.getId(), failedNodes, repairType, false, "variant", upgrade, rollingRestartEnabled);
        }
    }
}
