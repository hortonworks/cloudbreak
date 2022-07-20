package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
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
    private StackViewService stackViewService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ClusterRepairFlowEventChainFactory underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRepairSingleGatewayWithNoAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupStackView();
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
    public void testRepairSingleGatewayWithAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupStackView();
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
        setupStackView();
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
        setupStackView();

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
        setupStackView();

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
        setupStackView();

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
        setupStackView();

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
        setupStackView();
        setupPrimaryGateway();

        InstanceMetaData primaryGWInstanceMetadata = new InstanceMetaData();
        primaryGWInstanceMetadata.setDiscoveryFQDN("failedNode-FQDN-primary-gateway");
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Crn crn = mock(Crn.class);
        when(crn.getAccountId()).thenReturn("accountid");
        when(stackService.getCrnById(anyLong())).thenReturn(crn);
        ClusterRepairTriggerEvent triggerEvent = new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedSecondaryGateway()
                .with3FailedCore().withFailedAuxiliary().withOneNodeFromEachHostGroupAtOnce().build();
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
    public void testAddAwsNativeMigrationIfNeedWhenNotUpgrade() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        StackView stackView = setupStackView();
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(stackView.getId(), Map.of(), false, false);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeButNotAwsNativeVariantIsTheTriggered() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "triggeredVariant";
        StackView stackView = setupStackView();
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), Map.of(), false, triggeredVariant);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTheTriggeredButStackIsAlreadyOnAwsNativeVariant() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        StackView stackView = setupStackView();
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), Map.of(), false, triggeredVariant);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTheTriggeredOnTheLegacyAwsVariantAndEntitledForMigrationButNotEntitledFor() {
        when(entitlementService.awsVariantMigrationEnable(anyString())).thenReturn(false);
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        StackView stackView = setupStackView();
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), Map.of(), false, triggeredVariant);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariantIsTriggeredOnTheLegacyAwsVariantAndEntitledForMigration() {
        when(entitlementService.awsVariantMigrationEnable(anyString())).thenReturn(true);
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        String triggeredVariant = "AWS_NATIVE";
        StackView stackView = setupStackView();
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", stackView.getId(), Map.of(), false, triggeredVariant);

        underTest.addAwsNativeEventMigrationIfNeeded(flowTriggers, triggerEvent, groupName, stackView);

        Assertions.assertFalse(flowTriggers.isEmpty());
        AwsVariantMigrationTriggerEvent actual = (AwsVariantMigrationTriggerEvent) flowTriggers.peek();
        Assertions.assertEquals(groupName, actual.getHostGroupName());
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

    private StackView setupStackView() {
        StackView stack = mock(StackView.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getClusterView()).thenReturn(cluster);
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        Crn exampleCrn = CrnTestUtil.getDatalakeCrnBuilder()
                .setResource("aResource")
                .setAccountId("anAccountId")
                .build();
        when(stack.getResourceCrn()).thenReturn(exampleCrn.toString());
        when(stackViewService.getById(STACK_ID)).thenReturn(stack);
        return stack;
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

        private boolean oneNodeFromEachHostGroupAtOnce;

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

        private TriggerEventBuilder withFailedAuxiliary() {
            failedAuxiliaryNodes.add("aux1");
            return this;
        }

        private TriggerEventBuilder withOneNodeFromEachHostGroupAtOnce() {
            oneNodeFromEachHostGroupAtOnce = true;
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

            return new ClusterRepairTriggerEvent(stack.getId(), failedNodes, oneNodeFromEachHostGroupAtOnce, false);
        }
    }
}
