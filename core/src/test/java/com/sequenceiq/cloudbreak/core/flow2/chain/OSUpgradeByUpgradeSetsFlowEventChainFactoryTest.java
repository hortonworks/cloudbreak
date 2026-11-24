package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.LockedComponentsValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OSUpgradeByUpgradeSetsTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OrderedOsUpgradeRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowChainLog;

@ExtendWith(MockitoExtension.class)
public class OSUpgradeByUpgradeSetsFlowEventChainFactoryTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private OSUpgradeByUpgradeSetsFlowEventChainFactory underTest;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateFlowTriggerEventQueue(boolean dataHub) {
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
        when(stackView.isDatahub()).thenReturn(dataHub);
        when(stackDtoService.getStackViewById(1L)).thenReturn(stackView);

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(new OSUpgradeByUpgradeSetsTriggerEvent(1L, "AWS",
                new ImageChangeDto(1L, "imageId"), upgradeSets));

        if (dataHub) {
            LockedComponentsValidationTriggerEvent validation = (LockedComponentsValidationTriggerEvent) eventQueue.getQueue().remove();
            assertThat(validation.getImageId()).isEqualTo("imageId");
            assertThat(validation.isLockComponents()).isTrue();
            assertThat(validation.isReplaceVms()).isTrue();
        }
        assertEquals(4, eventQueue.getQueue().size());

        StackImageUpdateTriggerEvent firstFlow = (StackImageUpdateTriggerEvent) eventQueue.getQueue().poll();
        assertEquals("imageId", firstFlow.getNewImageId());
        assertEquals(1L, firstFlow.getResourceId());

        String persistedRepairs = TypedJsonUtil.writeValueAsStringSilent(eventQueue.getQueue());
        Queue<Selectable> restoredRepairs = new FlowChainLog(null, "chain", null, persistedRepairs, null, null).getChainAsQueue();
        assertThat(restoredRepairs).hasSize(3).allSatisfy(repair -> assertThat(repair).isExactlyInstanceOf(OrderedOsUpgradeRepairTriggerEvent.class));

        ClusterRepairTriggerEvent secondFlow = (ClusterRepairTriggerEvent) restoredRepairs.remove();
        assertEquals("CLUSTER_REPAIR_TRIGGER_EVENT", secondFlow.getSelector());
        assertEquals(ClusterRepairTriggerEvent.RepairType.ALL_AT_ONCE, secondFlow.getRepairType());
        Map<String, List<String>> hostGroupsWithHostNames = secondFlow.getFailedNodesMap();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("master", "worker");
        assertThat(hostGroupsWithHostNames.get("master")).containsExactlyInAnyOrder("host1");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host3", "host5");

        ClusterRepairTriggerEvent thirdFlow = (ClusterRepairTriggerEvent) restoredRepairs.remove();
        assertEquals("CLUSTER_REPAIR_TRIGGER_EVENT", thirdFlow.getSelector());
        assertEquals(ClusterRepairTriggerEvent.RepairType.ALL_AT_ONCE, thirdFlow.getRepairType());
        hostGroupsWithHostNames = thirdFlow.getFailedNodesMap();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("master", "worker");
        assertThat(hostGroupsWithHostNames.get("master")).containsExactlyInAnyOrder("host2");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host4");


        ClusterRepairTriggerEvent fourthFlow = (ClusterRepairTriggerEvent) restoredRepairs.remove();
        assertEquals("CLUSTER_REPAIR_TRIGGER_EVENT", fourthFlow.getSelector());
        assertEquals(ClusterRepairTriggerEvent.RepairType.ALL_AT_ONCE, fourthFlow.getRepairType());
        hostGroupsWithHostNames = fourthFlow.getFailedNodesMap();
        assertThat(hostGroupsWithHostNames).containsOnlyKeys("worker");
        assertThat(hostGroupsWithHostNames.get("worker")).containsExactlyInAnyOrder("host6");

        assertThat(secondFlow.getInstancesToRetainVolumes()).containsExactlyInAnyOrder("i-1", "i-3", "i-5");
        assertThat(thirdFlow.getInstancesToRetainVolumes()).containsExactlyInAnyOrder("i-2", "i-4");
        assertThat(fourthFlow.getInstancesToRetainVolumes()).containsExactly("i-6");
    }
}
