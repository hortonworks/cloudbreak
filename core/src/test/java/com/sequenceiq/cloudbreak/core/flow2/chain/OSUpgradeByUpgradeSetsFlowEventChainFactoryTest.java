package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OSUpgradeByUpgradeSetsTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
public class OSUpgradeByUpgradeSetsFlowEventChainFactoryTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterRepairService clusterRepairService;

    @InjectMocks
    private OSUpgradeByUpgradeSetsFlowEventChainFactory underTest;

    @Test
    public void testCreateFlowTriggerEventQueue() {
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3", "i-5")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-2", "i-4")));
        upgradeSets.add(new OrderedOSUpgradeSet(2, Set.of("i-6")));
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setGroupName("master");
        InstanceGroup workerGroup = new InstanceGroup();
        workerGroup.setGroupName("worker");
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN("host1");
        instanceMetaData1.setInstanceId("i-1");
        instanceMetaData1.setInstanceGroup(masterGroup);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setDiscoveryFQDN("host2");
        instanceMetaData2.setInstanceId("i-2");
        instanceMetaData2.setInstanceGroup(masterGroup);
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setDiscoveryFQDN("host3");
        instanceMetaData3.setInstanceId("i-3");
        instanceMetaData3.setInstanceGroup(workerGroup);
        InstanceMetaData instanceMetaData4 = new InstanceMetaData();
        instanceMetaData4.setDiscoveryFQDN("host4");
        instanceMetaData4.setInstanceId("i-4");
        instanceMetaData4.setInstanceGroup(workerGroup);
        InstanceMetaData instanceMetaData5 = new InstanceMetaData();
        instanceMetaData5.setDiscoveryFQDN("host5");
        instanceMetaData5.setInstanceId("i-5");
        instanceMetaData5.setInstanceGroup(workerGroup);
        InstanceMetaData instanceMetaData6 = new InstanceMetaData();
        instanceMetaData6.setDiscoveryFQDN("host6");
        instanceMetaData6.setInstanceId("i-6");
        instanceMetaData6.setInstanceGroup(workerGroup);
        when(instanceMetaDataService.findAllViewByStackIdAndInstanceId(1L, Set.of("i-1", "i-3", "i-5")))
                .thenReturn(List.of(instanceMetaData1, instanceMetaData3, instanceMetaData5));
        when(instanceMetaDataService.findAllViewByStackIdAndInstanceId(1L, Set.of("i-2", "i-4")))
                .thenReturn(List.of(instanceMetaData2, instanceMetaData4));
        when(instanceMetaDataService.findAllViewByStackIdAndInstanceId(1L, Set.of("i-6")))
                .thenReturn(List.of(instanceMetaData6));
        StackView stackView = mock(StackView.class);
        when(stackView.getEnvironmentCrn()).thenReturn("envcrn");
        when(stackView.getName()).thenReturn("envname");
        when(stackDtoService.getStackViewById(1L)).thenReturn(stackView);
        when(kerberosConfigService.isKerberosConfigExistsForEnvironment(stackView.getEnvironmentCrn(), stackView.getName())).thenReturn(true);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(1L)).thenReturn(Optional.of(instanceMetaData1));
        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(new OSUpgradeByUpgradeSetsTriggerEvent(1L, "AWS",
                new ImageChangeDto(1L, "imageId"), upgradeSets));
        assertEquals(7, eventQueue.getQueue().size());

        StackImageUpdateTriggerEvent firstFlow = (StackImageUpdateTriggerEvent) eventQueue.getQueue().poll();
        assertEquals("imageId", firstFlow.getNewImageId());
        assertEquals(1L, firstFlow.getResourceId());

        ClusterAndStackDownscaleTriggerEvent secondFlow = (ClusterAndStackDownscaleTriggerEvent) eventQueue.getQueue().poll();
        assertEquals("FULL_DOWNSCALE_TRIGGER_EVENT", secondFlow.getSelector());
        assertTrue(secondFlow.getDetails().isRepair());
        Map<String, Set<String>> hostGroupsWithHostNames = secondFlow.getHostGroupsWithHostNames();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("master", "worker");
        assertThat(hostGroupsWithHostNames.get("master")).containsExactlyInAnyOrder("host1");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host3", "host5");

        StackAndClusterUpscaleTriggerEvent thirdFlow = (StackAndClusterUpscaleTriggerEvent) eventQueue.getQueue().poll();
        hostGroupsWithHostNames = thirdFlow.getHostGroupsWithHostNames();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("master", "worker");
        assertThat(hostGroupsWithHostNames.get("master")).containsExactlyInAnyOrder("host1");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host3", "host5");
        Map<String, Integer> hostGroupsWithAdjustment = thirdFlow.getHostGroupsWithAdjustment();
        assertThat(hostGroupsWithAdjustment).containsExactlyInAnyOrderEntriesOf(Map.of("master", 1, "worker", 2));
        assertEquals("FULL_UPSCALE_TRIGGER_EVENT", thirdFlow.getSelector());

        ClusterAndStackDownscaleTriggerEvent fourthFlow = (ClusterAndStackDownscaleTriggerEvent) eventQueue.getQueue().poll();
        assertEquals("FULL_DOWNSCALE_TRIGGER_EVENT", fourthFlow.getSelector());
        assertTrue(fourthFlow.getDetails().isRepair());
        hostGroupsWithHostNames = fourthFlow.getHostGroupsWithHostNames();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("master", "worker");
        assertThat(hostGroupsWithHostNames.get("master")).containsExactlyInAnyOrder("host2");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host4");

        StackAndClusterUpscaleTriggerEvent fifthFlow = (StackAndClusterUpscaleTriggerEvent) eventQueue.getQueue().poll();
        hostGroupsWithHostNames = fifthFlow.getHostGroupsWithHostNames();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("master", "worker");
        assertThat(hostGroupsWithHostNames.get("master")).containsExactlyInAnyOrder("host2");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host4");
        assertEquals("FULL_UPSCALE_TRIGGER_EVENT", fifthFlow.getSelector());

        ClusterAndStackDownscaleTriggerEvent sixthFlow = (ClusterAndStackDownscaleTriggerEvent) eventQueue.getQueue().poll();
        assertTrue(sixthFlow.getDetails().isRepair());
        hostGroupsWithHostNames = sixthFlow.getHostGroupsWithHostNames();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("worker");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host6");

        StackAndClusterUpscaleTriggerEvent seventhFlow = (StackAndClusterUpscaleTriggerEvent) eventQueue.getQueue().poll();
        hostGroupsWithHostNames = seventhFlow.getHostGroupsWithHostNames();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("worker");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host6");
        assertEquals("FULL_UPSCALE_TRIGGER_EVENT", seventhFlow.getSelector());

        ArgumentCaptor<List<InstanceMetadataView>> instanceIdsToRepair = ArgumentCaptor.forClass(List.class);
        verify(clusterRepairService, times(3)).markVolumesToNonDeletable(eq(stackView), instanceIdsToRepair.capture());
        List<List<InstanceMetadataView>> instanceIdsToRepairAllValues = instanceIdsToRepair.getAllValues();
        List<InstanceMetadataView> instanceMetadataViews = instanceIdsToRepairAllValues.stream().flatMap(Collection::stream).collect(Collectors.toList());
        assertThat(instanceMetadataViews).extracting("instanceId").containsExactlyInAnyOrder("i-1", "i-2", "i-3", "i-4", "i-5", "i-6");
    }
}