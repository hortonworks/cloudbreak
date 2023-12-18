package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
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

public class ClusterRepairFlowEventChainFactoryTest {

    private static final long INSTANCE_GROUP_ID = 1L;

    private static final String FAILED_NODE_FQDN_PRIMARY_GATEWAY = "failedNode-FQDN-primary-gateway";

    private static final String FAILED_NODE_FQDN_SECONDARY_GATEWAY = "failedNode-FQDN-secondary-gateway";

    private static final String FAILED_NODE_FQDN_CORE = "failedNode-FQDN-core";

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final boolean MULTIPLE_GATEWAY = true;

    private static final boolean NOT_MULTIPLE_GATEWAY = false;

    private static final StackIdView ATTACHED_WORKLOAD = mock(StackIdView.class);

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
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayWithNoAttachedWithEmbeddedDBUpgrade() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        when(entitlementService.isEmbeddedPostgresUpgradeEnabled(anyString())).thenReturn(true);
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(true);
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withUpgrade().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "UPGRADE_EMBEDDEDDB_PREPARATION_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "UPGRADE_RDS_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayWithAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayMultipleNodes() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());

        HostGroup masterHostGroup = setupHostGroup("hostGroup-master", setupInstanceGroup(InstanceGroupType.GATEWAY));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-master"))).thenReturn(Optional.of(masterHostGroup));
        HostGroup coreHostGroup = setupHostGroup("hostGroup-core", setupInstanceGroup(InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-core"))).thenReturn(Optional.of(coreHostGroup));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"),
                triggeredOperations);
    }

    @Test
    public void testRepairMultipleGatewayWithNoAttached() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        InstanceGroupView ig = mock(InstanceGroupView.class);
        when(ig.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        when(ig.getNodeCount()).thenReturn(5);
        when(instanceGroupService.findViewByStackId(STACK_ID)).thenReturn(Set.of(ig));
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairMultipleGatewayWithAttached() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        InstanceGroupView ig = mock(InstanceGroupView.class);
        when(ig.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        when(ig.getNodeCount()).thenReturn(5);
        when(instanceGroupService.findViewByStackId(STACK_ID)).thenReturn(Set.of(ig));
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairCoreNodes() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(false);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairNotGatewayInstanceGroup() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(false);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairOneNodeFromEachHostGroupAtOnce() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        HostGroup masterHostGroup = setupHostGroup("hostGroup-master", setupInstanceGroup(InstanceGroupType.GATEWAY));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-master"))).thenReturn(Optional.of(masterHostGroup));
        HostGroup coreHostGroup = setupHostGroup("hostGroup-core", setupInstanceGroup(InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-core"))).thenReturn(Optional.of(coreHostGroup));
        HostGroup auxiliaryHostGroup = setupHostGroup("hostGroup-auxiliary", setupInstanceGroup(InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-auxiliary"))).thenReturn(Optional.of(auxiliaryHostGroup));
        when(entitlementService.isDatalakeZduOSUpgradeEnabled(anyString())).thenReturn(true);
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN("failedNode-FQDN-primary-gateway");
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Crn crn = mock(Crn.class);
        when(crn.getAccountId()).thenReturn("accountid");
        when(stackService.getCrnById(anyLong())).thenReturn(crn);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedSecondaryGateway()
                .with3FailedCore().withFailedAuxiliary().withRepairType(RepairType.ONE_FROM_EACH_HOSTGROUP).build();
        FlowTriggerEventQueue eventQueues =
                underTest.createFlowTriggerEventQueue(triggerEvent);
        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);

        Set<String> downscaledMasterHosts = new HashSet<>();
        Set<String> downscaledCoreHosts = new HashSet<>();
        Set<String> downscaledAuxHosts = new HashSet<>();
        Set<String> upscaledMasterHosts = new HashSet<>();
        Set<String> upscaledCoreHosts = new HashSet<>();
        Set<String> upscaledAuxHosts = new HashSet<>();

        eventQueues.getQueue().remove();
        StackDownscaleTriggerEvent downscale1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> firstDownscaledHostsInMaster = downscale1.getHostGroupsWithHostNames().get("hostGroup-master");
        assertEquals(1, firstDownscaledHostsInMaster.size());
        String firstDownscaledMasterHost = firstDownscaledHostsInMaster.iterator().next();
        downscaledMasterHosts.add(firstDownscaledMasterHost);
        assertEquals("failedNode-FQDN-primary-gateway", firstDownscaledMasterHost);
        Set<String> firstDownscaledHostsInCore = downscale1.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(1, firstDownscaledHostsInCore.size());
        String firstDownscaleCoreHost = firstDownscaledHostsInCore.iterator().next();
        downscaledCoreHosts.add(firstDownscaleCoreHost);
        Set<String> firstDownscaledHostsInAux = downscale1.getHostGroupsWithHostNames().get("hostGroup-auxiliary");
        assertEquals(1, firstDownscaledHostsInAux.size());
        String downscaledAuxHost = firstDownscaledHostsInAux.iterator().next();
        downscaledAuxHosts.add(downscaledAuxHost);
        StackAndClusterUpscaleTriggerEvent upscale1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> firstUpscaledHostsInMaster = upscale1.getHostGroupsWithHostNames().get("hostGroup-master");
        assertEquals(1, firstUpscaledHostsInMaster.size());
        String firstUpscaledMasterHost = firstUpscaledHostsInMaster.iterator().next();
        assertEquals("failedNode-FQDN-primary-gateway", firstUpscaledMasterHost);
        upscaledMasterHosts.add(firstUpscaledMasterHost);
        Set<String> firstUpscaledHostsInCore = upscale1.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(1, firstUpscaledHostsInCore.size());
        upscaledCoreHosts.add(firstUpscaledHostsInCore.iterator().next());
        Set<String> firstUpscaledHostsInAux = upscale1.getHostGroupsWithHostNames().get("hostGroup-auxiliary");
        assertEquals(1, firstUpscaledHostsInAux.size());
        upscaledAuxHosts.add(firstUpscaledHostsInAux.iterator().next());

        ClusterAndStackDownscaleTriggerEvent downscale2 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> secondDownscaledHostsInMaster = downscale2.getHostGroupsWithHostNames().get("hostGroup-master");
        assertEquals(1, secondDownscaledHostsInMaster.size());
        String secondDownscaledMasterHost = secondDownscaledHostsInMaster.iterator().next();
        downscaledMasterHosts.add(secondDownscaledMasterHost);
        assertEquals("failedNode-FQDN-secondary-gateway", secondDownscaledMasterHost);
        Set<String> secondDownscaledHostsInCore = downscale2.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(1, secondDownscaledHostsInCore.size());
        String secondDownscaleCoreHost = secondDownscaledHostsInCore.iterator().next();
        downscaledCoreHosts.add(secondDownscaleCoreHost);
        StackAndClusterUpscaleTriggerEvent upscale2 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> secondUpscaledHostsInMaster = upscale2.getHostGroupsWithHostNames().get("hostGroup-master");
        assertEquals(1, secondUpscaledHostsInMaster.size());
        String secondUpscaledMasterHost = secondUpscaledHostsInMaster.iterator().next();
        assertEquals("failedNode-FQDN-secondary-gateway", secondUpscaledMasterHost);
        upscaledMasterHosts.add(secondUpscaledMasterHost);
        Set<String> secondUpscaledHostsInCore = upscale2.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(1, secondUpscaledHostsInCore.size());
        upscaledCoreHosts.add(secondUpscaledHostsInCore.iterator().next());

        ClusterAndStackDownscaleTriggerEvent downscale3 = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> thirdDownscaledHostsInCore = downscale3.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(1, thirdDownscaledHostsInCore.size());
        String thirdDownscaleCoreHost = thirdDownscaledHostsInCore.iterator().next();
        downscaledCoreHosts.add(thirdDownscaleCoreHost);
        StackAndClusterUpscaleTriggerEvent upscale3 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> thirdUpscaledHostsInCore = upscale3.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(1, thirdUpscaledHostsInCore.size());
        upscaledCoreHosts.add(thirdUpscaledHostsInCore.iterator().next());

        assertThat(downscaledCoreHosts, containsInAnyOrder("core1", "core2", "core3"));
        assertThat(downscaledMasterHosts, containsInAnyOrder(FAILED_NODE_FQDN_PRIMARY_GATEWAY, FAILED_NODE_FQDN_SECONDARY_GATEWAY));
        assertThat(downscaledAuxHosts, containsInAnyOrder("aux1"));
        assertThat(upscaledCoreHosts, containsInAnyOrder("core1", "core2", "core3"));
        assertThat(upscaledMasterHosts, containsInAnyOrder(FAILED_NODE_FQDN_PRIMARY_GATEWAY, FAILED_NODE_FQDN_SECONDARY_GATEWAY));
        assertThat(upscaledAuxHosts, containsInAnyOrder("aux1"));
    }

    @Test
    public void testBatchRepair() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        HostGroup masterHostGroup = setupHostGroup("hostGroup-master", setupInstanceGroup(InstanceGroupType.GATEWAY));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-master"))).thenReturn(Optional.of(masterHostGroup));
        HostGroup coreHostGroup = setupHostGroup("hostGroup-core", setupInstanceGroup(InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-core"))).thenReturn(Optional.of(coreHostGroup));
        HostGroup auxiliaryHostGroup = setupHostGroup("hostGroup-auxiliary", setupInstanceGroup(InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-auxiliary"))).thenReturn(Optional.of(auxiliaryHostGroup));
        when(entitlementService.isDatalakeZduOSUpgradeEnabled(anyString())).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN("failedNode-FQDN-primary-gateway");
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Crn crn = mock(Crn.class);
        when(crn.getAccountId()).thenReturn("accountid");
        when(stackService.getCrnById(anyLong())).thenReturn(crn);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedSecondaryGateway()
                .with320FailedCore().withFailedAuxiliary().withRepairType(RepairType.BATCH).build();
        FlowTriggerEventQueue eventQueues =
                underTest.createFlowTriggerEventQueue(triggerEvent);
        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT", "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);

        Set<String> downscaledCoreHosts = new HashSet<>();
        Set<String> downscaledAuxHosts = new HashSet<>();
        Set<String> upscaledCoreHosts = new HashSet<>();
        Set<String> upscaledAuxHosts = new HashSet<>();

        eventQueues.getQueue().remove();
        StackDownscaleTriggerEvent downscale1 = (StackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> firstDownscaledHostsInMaster = downscale1.getHostGroupsWithHostNames().get("hostGroup-master");
        assertEquals(1, firstDownscaledHostsInMaster.size());
        String firstDownscaledMasterHost = firstDownscaledHostsInMaster.iterator().next();
        assertEquals("failedNode-FQDN-primary-gateway", firstDownscaledMasterHost);
        Set<String> firstDownscaledHostsInCore = downscale1.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(98, firstDownscaledHostsInCore.size());
        String firstDownscaleCoreHost = firstDownscaledHostsInCore.iterator().next();
        downscaledCoreHosts.add(firstDownscaleCoreHost);
        Set<String> firstDownscaledHostsInAux = downscale1.getHostGroupsWithHostNames().get("hostGroup-auxiliary");
        assertEquals(1, firstDownscaledHostsInAux.size());
        String downscaledAuxHost = firstDownscaledHostsInAux.iterator().next();
        downscaledAuxHosts.add(downscaledAuxHost);
        StackAndClusterUpscaleTriggerEvent upscale1 = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        Set<String> firstUpscaledHostsInMaster = upscale1.getHostGroupsWithHostNames().get("hostGroup-master");
        assertEquals(1, firstUpscaledHostsInMaster.size());
        String firstUpscaledMasterHost = firstUpscaledHostsInMaster.iterator().next();
        assertEquals("failedNode-FQDN-primary-gateway", firstUpscaledMasterHost);
        Set<String> firstUpscaledHostsInCore = upscale1.getHostGroupsWithHostNames().get("hostGroup-core");
        assertEquals(98, firstUpscaledHostsInCore.size());
        upscaledCoreHosts.add(firstUpscaledHostsInCore.iterator().next());
        Set<String> firstUpscaledHostsInAux = upscale1.getHostGroupsWithHostNames().get("hostGroup-auxiliary");
        assertEquals(1, firstUpscaledHostsInAux.size());
        upscaledAuxHosts.add(firstUpscaledHostsInAux.iterator().next());

        ClusterAndStackDownscaleTriggerEvent downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        StackAndClusterUpscaleTriggerEvent upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        checkGroup(downscaleEvent, upscaleEvent, "hostGroup-core", downscaledCoreHosts, upscaledCoreHosts, 100);
        assertHosts(upscaledCoreHosts, 98, 100);
        downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        checkGroup(downscaleEvent, upscaleEvent, "hostGroup-core", downscaledCoreHosts, upscaledCoreHosts, 100);
        assertHosts(upscaledCoreHosts, 198, 100);
        downscaleEvent = (ClusterAndStackDownscaleTriggerEvent) eventQueues.getQueue().poll();
        upscaleEvent = (StackAndClusterUpscaleTriggerEvent) eventQueues.getQueue().poll();
        checkGroup(downscaleEvent, upscaleEvent, "hostGroup-core", downscaledCoreHosts, upscaledCoreHosts, 22);
        assertHosts(upscaledCoreHosts, 298, 22);
        Set<String> downscaledMasterHosts = new HashSet<>();
        Set<String> upscaledMasterHosts = new HashSet<>();
        checkGroup(downscaleEvent, upscaleEvent, "hostGroup-master", downscaledMasterHosts, upscaledMasterHosts, 1);
        assertThat(downscaledMasterHosts, containsInAnyOrder(FAILED_NODE_FQDN_SECONDARY_GATEWAY));
        assertThat(upscaledMasterHosts, containsInAnyOrder(FAILED_NODE_FQDN_SECONDARY_GATEWAY));
    }

    private static void assertHosts(Set<String> upscaledCoreHosts, int startNodeNumber, int nodeCount) {
        Set<String> coreHosts = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            coreHosts.add("core-" + (startNodeNumber + i));
        }
        assertEquals(upscaledCoreHosts, coreHosts);
    }

    private static void checkGroup(ClusterAndStackDownscaleTriggerEvent downscaleTriggerEvent, StackAndClusterUpscaleTriggerEvent upscaleTriggerEvent,
            String group, Set<String> downscaledHosts, Set<String> upscaledHosts, int size) {
        checkDownscale(downscaleTriggerEvent, group, size, downscaledHosts);
        checkUpscale(upscaleTriggerEvent, group, size, upscaledHosts);
    }

    private static void checkDownscale(ClusterAndStackDownscaleTriggerEvent downscaleEvent,
            String key, int size, Set<String> downscaledHosts) {
        Set<String> downscaledHostsFromGroup = downscaleEvent.getHostGroupsWithHostNames().get(key);
        assertEquals(size, downscaledHostsFromGroup.size());
        downscaledHosts.clear();
        downscaledHosts.addAll(downscaledHostsFromGroup);
    }

    private static void checkUpscale(StackAndClusterUpscaleTriggerEvent upscaleEvent,
            String key, int size, Set<String> upscaledHosts) {
        Set<String> upscaledHostsFromGroup = upscaleEvent.getHostGroupsWithHostNames().get(key);
        assertEquals(size, upscaledHostsFromGroup.size());
        upscaledHosts.clear();
        upscaledHosts.addAll(upscaledHostsFromGroup);
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
                false, triggeredVariant);

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
                Map.of(), false, triggeredVariant);

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
                Map.of(), false, triggeredVariant);

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
                Map.of(), false, triggeredVariant);

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
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(false);
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        com.sequenceiq.cloudbreak.view.InstanceGroupView instanceGroupView = mock(com.sequenceiq.cloudbreak.view.InstanceGroupView.class);
        when(instanceGroupView.getGroupName()).thenReturn("core");
        Template template = mock(Template.class);
        when(template.getRootVolumeSize()).thenReturn(100);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto));

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());
        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_VERTICAL_SCALE_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);

        eventQueues.getQueue().remove();
        CoreVerticalScalingTriggerEvent coreVerticalScalingTriggerEvent = (CoreVerticalScalingTriggerEvent) eventQueues.getQueue().poll();
        assertEquals(200, coreVerticalScalingTriggerEvent.getRequest().getTemplate().getRootVolume().getSize().intValue());
    }

    private void setupHostGroup(boolean gatewayInstanceGroup) {
        String hostGroupName = gatewayInstanceGroup ? "gateway" : "core";
        HostGroup hostGroup = setupHostGroup(hostGroupName, setupInstanceGroup(gatewayInstanceGroup ? InstanceGroupType.GATEWAY : InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));
        setupPrimaryGateway();
    }

    private void setupPrimaryGateway() {
        when(instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(anyLong(), anyLong()))
                .thenReturn(Optional.of(FAILED_NODE_FQDN_PRIMARY_GATEWAY));
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
        return hostGroup;
    }

    private InstanceGroup setupInstanceGroup(InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getId()).thenReturn(INSTANCE_GROUP_ID);
        when(instanceGroup.getInstanceGroupType()).thenReturn(instanceGroupType);
        return instanceGroup;
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
            failedGatewayNodes.add(FAILED_NODE_FQDN_PRIMARY_GATEWAY);
            return this;
        }

        private TriggerEventBuilder withFailedSecondaryGateway() {
            failedGatewayNodes.add(FAILED_NODE_FQDN_SECONDARY_GATEWAY);
            return this;
        }

        private TriggerEventBuilder withFailedCore() {
            failedCoreNodes.add(FAILED_NODE_FQDN_CORE);
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
            Map<String, List<String>> failedNodes = new HashMap<>();
            if (!failedGatewayNodes.isEmpty()) {
                failedNodes.put("hostGroup-master", failedGatewayNodes);
            }
            if (!failedCoreNodes.isEmpty()) {
                failedNodes.put("hostGroup-core", failedCoreNodes);
            }
            if (!failedAuxiliaryNodes.isEmpty()) {
                failedNodes.put("hostGroup-auxiliary", failedAuxiliaryNodes);
            }

            return new ClusterRepairTriggerEvent(stack.getId(), failedNodes, repairType, false, "variant", upgrade);
        }
    }
}
