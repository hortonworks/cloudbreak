package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RescheduleStatusCheckTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

public class ClusterRepairFlowEventChainFactoryTest {

    private static final long INSTANCE_GROUP_ID = 1L;

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final boolean MULTIPLE_GATEWAY = true;

    private static final boolean NOT_MULTIPLE_GATEWAY = false;

    private static final String HG_MASTER = "hostGroup-master";

    private static final String HG_CORE = "hostGroup-core";

    private static final String HG_AUXILIARY = "hostGroup-auxiliary";

    private static final String FAILED_PRIMARY_GATEWAY_FQDN = "failedNode-FQDN-primary-gateway";

    private static final String FAILED_SECONDARY_GATEWAY_FQDN = "failedNode-FQDN-secondary-gateway";

    private static final String FAILED_CORE_FQDN = "failedNode-FQDN-core";

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

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

    @InjectMocks
    private ClusterRepairFlowEventChainFactory underTest;

    @Mock
    private StackIdView attachedWorkload;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(underTest, "targetMajorVersion", TargetMajorVersion.VERSION_11);
        when(entitlementService.isEmbeddedPostgresUpgradeEnabled(anyString())).thenReturn(false);
        setupStackDto();
        setupViews();
    }

    @Test
    public void testRepairSingleGatewayWithNoAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairSingleGatewayWithNoAttachedWithEmbeddedDBUpgrade() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        when(entitlementService.isEmbeddedPostgresUpgradeEnabled(anyString())).thenReturn(true);
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(true);
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withUpgrade().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "UPGRADE_EMBEDDEDDB_PREPARATION_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "UPGRADE_RDS_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairSingleGatewayWithAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupConnectedCluster();
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairSingleGatewayMultipleNodes() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairMultipleGatewayWithNoAttached() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupInstanceGroup(InstanceGroupType.GATEWAY, 5);
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairMultipleGatewayWithAttached() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        setupConnectedCluster();
        setupInstanceGroup(InstanceGroupType.GATEWAY, 5);
        setupHostGroup(InstanceGroupType.GATEWAY);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairCoreNodes() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupConnectedCluster();
        setupHostGroup(InstanceGroupType.CORE);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairNotGatewayInstanceGroup() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupHostGroup(InstanceGroupType.CORE);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));
    }

    @Test
    public void testRepairOneNodeFromEachHostGroupAtOnce() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupHostGroup(HG_AUXILIARY, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN(FAILED_PRIMARY_GATEWAY_FQDN);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Crn crn = mock(Crn.class);
        when(crn.getAccountId()).thenReturn("accountid");
        when(stackService.getCrnById(anyLong())).thenReturn(crn);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack)
                .withFailedPrimaryGateway()
                .withFailedSecondaryGateway()
                .with3FailedCore()
                .withFailedAuxiliary()
                .withRepairType(RepairType.ONE_FROM_EACH_HOSTGROUP)
                .build();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        assertThat(eventQueues.getQueue(), hasSize(9));
        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainInitPayload);

        StackDownscaleTriggerEvent downscale1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);
        assertGroupWithHost(downscale1, HG_CORE, "core1");
        assertGroupWithHost(downscale1, HG_AUXILIARY, "aux1");

        StackAndClusterUpscaleTriggerEvent upscale1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);
        assertGroupWithHost(upscale1, HG_CORE, "core1");
        assertGroupWithHost(upscale1, HG_AUXILIARY, "aux1");

        ClusterAndStackDownscaleTriggerEvent downscale2 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale2, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN);
        assertGroupWithHost(downscale2, HG_CORE, "core2");

        StackAndClusterUpscaleTriggerEvent upscale2 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale2, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN);
        assertGroupWithHost(upscale2, HG_CORE, "core2");

        ClusterAndStackDownscaleTriggerEvent downscale3 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale3, HG_CORE, "core3");

        StackAndClusterUpscaleTriggerEvent upscale3 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale3, HG_CORE, "core3");

        RescheduleStatusCheckTriggerEvent statusCheckTriggerEvent = (RescheduleStatusCheckTriggerEvent) eventQueues.getQueue().poll();
        assertNotNull(statusCheckTriggerEvent);

        FlowChainFinalizePayload flowChainFinalizePayload = (FlowChainFinalizePayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainFinalizePayload);
    }

    @Test
    public void testRepairNodesOneByOne() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupHostGroup(HG_AUXILIARY, setupInstanceGroup(InstanceGroupType.CORE));
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN(FAILED_PRIMARY_GATEWAY_FQDN);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Crn crn = mock(Crn.class);
        when(crn.getAccountId()).thenReturn("accountid");
        when(stackService.getCrnById(anyLong())).thenReturn(crn);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack)
                .withFailedPrimaryGateway()
                .withFailedSecondaryGateway()
                .with3FailedCore()
                .withFailedAuxiliary()
                .withRepairType(RepairType.ONE_BY_ONE)
                .build();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        assertThat(eventQueues.getQueue(), hasSize(15));
        FlowChainInitPayload flowChainInitPayload = (FlowChainInitPayload) eventQueues.getQueue().poll();
        assertNotNull(flowChainInitPayload);

        StackDownscaleTriggerEvent downscaleMaster1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscaleMaster1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);
        StackAndClusterUpscaleTriggerEvent upscaleMaster1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscaleMaster1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);

        ClusterAndStackDownscaleTriggerEvent downscaleMaster2 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscaleMaster2, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN);
        StackAndClusterUpscaleTriggerEvent upscaleMaster2 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscaleMaster2, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN);

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
    }

    @Test
    public void testBatchRepair() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        setupNoConnectedCluster();
        setupHostGroup(HG_MASTER, setupInstanceGroup(InstanceGroupType.GATEWAY));
        setupHostGroup(HG_CORE, setupInstanceGroup(InstanceGroupType.CORE));
        setupHostGroup(HG_AUXILIARY, setupInstanceGroup(InstanceGroupType.CORE));
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN(FAILED_PRIMARY_GATEWAY_FQDN);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Crn crn = mock(Crn.class);
        when(crn.getAccountId()).thenReturn("accountid");
        when(stackService.getCrnById(anyLong())).thenReturn(crn);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack)
                .withFailedPrimaryGateway()
                .withFailedSecondaryGateway()
                .with320FailedCore()
                .withFailedAuxiliary()
                .withRepairType(RepairType.BATCH)
                .build();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEvents(eventQueues, List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT", "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        eventQueues.getQueue().remove();

        StackDownscaleTriggerEvent downscale1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscale1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);
        assertGroupWithHosts(downscale1, HG_CORE, hosts("core-", 0, 98));
        assertGroupWithHost(downscale1, HG_AUXILIARY, "aux1");

        StackAndClusterUpscaleTriggerEvent upscale1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscale1, HG_MASTER, FAILED_PRIMARY_GATEWAY_FQDN);
        assertGroupWithHosts(upscale1, HG_CORE, hosts("core-", 0, 98));
        assertGroupWithHost(upscale1, HG_AUXILIARY, "aux1");

        ClusterAndStackDownscaleTriggerEvent downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscaleEvent, HG_CORE, hosts("core-", 98, 198));
        StackAndClusterUpscaleTriggerEvent upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscaleEvent, HG_CORE, hosts("core-", 98, 198));

        downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(downscaleEvent, HG_CORE, hosts("core-", 198, 298));
        upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHosts(upscaleEvent, HG_CORE, hosts("core-", 198, 298));

        downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(downscaleEvent, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN);
        assertGroupWithHosts(downscaleEvent, HG_CORE, hosts("core-", 298, 320));

        upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        assertGroupWithHost(upscaleEvent, HG_MASTER, FAILED_SECONDARY_GATEWAY_FQDN);
        assertGroupWithHosts(upscaleEvent, HG_CORE, hosts("core-", 298, 320));
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenNotUpgrade() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(stackView.getId(), Map.of(), RepairType.ALL_AT_ONCE, false, "variant");

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeButNotAwsNativeVariantIsTheTriggered() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "triggeredVariant";
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), RepairType.ALL_AT_ONCE, Map.of(),
                false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTheTriggeredButStackIsAlreadyOnAwsNativeVariant() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTheTriggeredOnTheLegacyAwsVariantAndEntitledForMigrationButNotEntitledFor() {
        when(entitlementService.awsVariantMigrationEnable(anyString())).thenReturn(false);
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTriggeredOnTheLegacyAwsVariantAndEntitledForMigration() {
        when(entitlementService.awsVariantMigrationEnable(anyString())).thenReturn(true);
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "AWS_NATIVE";
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(true);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertFalse(flowTriggers.isEmpty());
        AwsVariantMigrationTriggerEvent actual = (AwsVariantMigrationTriggerEvent) flowTriggers.peek();
        Assertions.assertEquals(groupName, actual.getHostGroupName());
    }

    @Test
    public void testRootDiskMigration() {
        ReflectionTestUtils.setField(underTest, "rootDiskRepairMigrationEnabled", true);
        when(rootVolumeSizeProvider.getForPlatform(any())).thenReturn(200);
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupConnectedCluster();
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
                "STACK_VERTICAL_SCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"));

        eventQueues.getQueue().remove();
        CoreVerticalScalingTriggerEvent coreVerticalScalingTriggerEvent = (CoreVerticalScalingTriggerEvent) eventQueues.getQueue().poll();
        assertEquals(200, coreVerticalScalingTriggerEvent.getRequest().getTemplate().getRootVolume().getSize().intValue());
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
        when(instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(anyLong(), anyLong()))
                .thenReturn(Optional.of(FAILED_PRIMARY_GATEWAY_FQDN));
    }

    private Stack getStack(boolean multipleGateway) {
        Stack stack = mock(Stack.class);
        when(stack.isMultipleGateway()).thenReturn(multipleGateway);
        Cluster cluster = mock(Cluster.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        return stack;
    }

    private void setupViews() {
        when(stackView.getId()).thenReturn(STACK_ID);
        Crn exampleCrn = CrnTestUtil.getDatalakeCrnBuilder()
                .setResource("aResource")
                .setAccountId("anAccountId")
                .build();
        when(stackView.getResourceCrn()).thenReturn(exampleCrn.toString());
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
    }

    private void setupStackDto() {
        Crn exampleCrn = CrnTestUtil.getDatalakeCrnBuilder()
                .setResource("aResource")
                .setAccountId("anAccountId")
                .build();
        when(stackDto.getResourceCrn()).thenReturn(exampleCrn.toString());
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(false);
    }

    private HostGroup setupHostGroup(String hostGroupName, InstanceGroup instanceGroup) {
        HostGroup hostGroup = mock(HostGroup.class);
        when(hostGroup.getName()).thenReturn(hostGroupName);
        when(hostGroup.getInstanceGroup()).thenReturn(instanceGroup);
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq(hostGroupName))).thenReturn(Optional.of(hostGroup));
        return hostGroup;
    }

    private InstanceGroup setupInstanceGroup(InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getId()).thenReturn(INSTANCE_GROUP_ID);
        when(instanceGroup.getInstanceGroupType()).thenReturn(instanceGroupType);
        return instanceGroup;
    }

    private void setupInstanceGroup(InstanceGroupType instanceGroupType, int nodeCount) {
        InstanceGroupView ig = mock(InstanceGroupView.class);
        when(ig.getInstanceGroupType()).thenReturn(instanceGroupType);
        when(ig.getNodeCount()).thenReturn(nodeCount);
        when(instanceGroupService.findViewByStackId(STACK_ID)).thenReturn(Set.of(ig));
    }

    private void setupNoConnectedCluster() {
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
    }

    private void setupConnectedCluster() {
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(attachedWorkload));
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

        private TriggerEventBuilder(Stack stack) {
            this.stack = stack;
        }

        private TriggerEventBuilder withFailedPrimaryGateway() {
            failedGatewayNodes.add(FAILED_PRIMARY_GATEWAY_FQDN);
            return this;
        }

        private TriggerEventBuilder withFailedSecondaryGateway() {
            failedGatewayNodes.add(FAILED_SECONDARY_GATEWAY_FQDN);
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

            return new ClusterRepairTriggerEvent(stack.getId(), failedNodes, repairType, false, "variant", upgrade);
        }
    }
}
