package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class StopStartDownscaleDecommissionViaCMHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Long STACK_ID = 100L;

    private static final Long CLUSTER_ID = 101L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackService stackService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private StopStartDownscaleDecommissionViaCMHandler underTest;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterDecomissionService clusterDecomissionService;

    @BeforeEach
    void setUp() {
        setupBasicMocks();
    }

    @Test
    void testAllDecommissioned() {
        testCollectDecommissionCombinationsInternal(5, 5, 5);
    }

    @Test
    void testCmHasMissingNodes() {
        testCollectDecommissionCombinationsInternal(5, 4, 4);
    }

    @Test
    void testCollectAndDecommissionReturnFewerNodes() {
        testCollectDecommissionCombinationsInternal(5, 4, 3);
    }

    @Test
    void testDecommissionReturnsFewerNodes() {
        testCollectDecommissionCombinationsInternal(5, 5, 3);
    }

    @Test
    void testNoNodesAvailableInCm() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 0;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetaData> instancesToDecommission = getInstancesToDecommission(instancesToDecommissionCount);
        HostGroup hostGroup = createHostGroup(instancesToDecommission);
        Map<String, InstanceMetaData> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetaData> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(hostGroup, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instanceIdsToDecommission);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verifyNoMoreInteractions(instanceMetaDataService);

        verify(clusterDecomissionService).collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission));
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testNoNodesFromCMDecommission() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 4;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetaData> instancesToDecommission = getInstancesToDecommission(instancesToDecommissionCount);
        HostGroup hostGroup = createHostGroup(instancesToDecommission);
        Map<String, InstanceMetaData> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetaData> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(hostGroup, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instanceIdsToDecommission);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verifyNoMoreInteractions(instanceMetaDataService);

        verify(clusterDecomissionService).collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testNoNodesInInitialDecommissionRequest() {
        int instancesToDecommissionCount = 0;
        int expcetedInstanceToCollectCount = 0;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetaData> instancesToDecommission = getInstancesToDecommission(instancesToDecommissionCount);
        HostGroup hostGroup = createHostGroup(instancesToDecommission);
        Map<String, InstanceMetaData> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetaData> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(hostGroup, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instanceIdsToDecommission);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verify(clusterDecomissionService).collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission));
        verifyNoMoreInteractions(instanceMetaDataService);
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testErrorFromCmHostCollection() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        List<InstanceMetaData> instancesToDecommission = getInstancesToDecommission(instancesToDecommissionCount);
        HostGroup hostGroup = createHostGroup(instancesToDecommission);
        Map<String, InstanceMetaData> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetaData> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(hostGroup, instancesToDecommission, collected, fqdnsDecommissioned);
        when(clusterDecomissionService.collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission)))
                .thenThrow(new RuntimeException("collectHostsToDecommissionError"));

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instanceIdsToDecommission);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        verify(clusterDecomissionService).collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission));

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getErrorDetails().getMessage()).isEqualTo("collectHostsToDecommissionError");
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.selector()).isEqualTo("STOPSTARTDOWNSCALEDECOMMISSIONVIACMRESULT_ERROR");

        verifyNoMoreInteractions(instanceMetaDataService);
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testErrorFromCmCommission() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        List<InstanceMetaData> instancesToDecommission = getInstancesToDecommission(instancesToDecommissionCount);
        HostGroup hostGroup = createHostGroup(instancesToDecommission);
        Map<String, InstanceMetaData> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetaData> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(hostGroup, instancesToDecommission, collected, fqdnsDecommissioned);
        when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collected), anyLong()))
                .thenThrow(new RuntimeException("decommissionHostsError"));

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instanceIdsToDecommission);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        verify(clusterDecomissionService).collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getErrorDetails().getMessage()).isEqualTo("decommissionHostsError");
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.selector()).isEqualTo("STOPSTARTDOWNSCALEDECOMMISSIONVIACMRESULT_ERROR");

        verifyNoMoreInteractions(instanceMetaDataService);
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    private void testCollectDecommissionCombinationsInternal(int instancesToDecommissionCount, int expcetedInstanceToCollectCount,
            int expectedInstancesDecommissionedCount) {
        List<InstanceMetaData> instancesToDecommission = getInstancesToDecommission(instancesToDecommissionCount);
        HostGroup hostGroup = createHostGroup(instancesToDecommission);
        Map<String, InstanceMetaData> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetaData> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(hostGroup, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instanceIdsToDecommission);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        for (InstanceMetaData instanceMetaData : decommissionedMetadataList) {
            verify(instanceMetaDataService).updateInstanceStatus(eq(instanceMetaData), eq(InstanceStatus.DECOMMISSIONED), anyString());
        }
        verifyNoMoreInteractions(instanceMetaDataService);

        verify(clusterDecomissionService).collectHostsToRemove(eq(hostGroup), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verify(clusterDecomissionService).enterMaintenanceMode(eq(stack), eq(fqdnsDecommissioned));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    private void setupBasicMocks() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);

        lenient().when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
    }

    private void setupAdditionalMocks(HostGroup hostGroup, List<InstanceMetaData> allInstanceMetadata,
            Map<String, InstanceMetaData> collectedInstances, Set<String> decommissionedInstances) {
        lenient().when(stack.getInstanceMetaDataAsList()).thenReturn(allInstanceMetadata);
        lenient().when(stackService.getInstanceMetadata(any(), any())).thenCallRealMethod();

        lenient().when(hostGroupService.getByClusterIdAndName(eq(CLUSTER_ID), eq(INSTANCE_GROUP_NAME))).thenReturn(Optional.of(hostGroup));

        Set<String> hostnames = allInstanceMetadata.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        lenient().when(clusterDecomissionService.collectHostsToRemove(eq(hostGroup), eq(hostnames))).thenReturn(collectedInstances);

        lenient().when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collectedInstances), anyLong())).thenReturn(decommissionedInstances);
    }

    private List<InstanceMetaData> getInstancesToDecommission(int count) {
        List<InstanceMetaData> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        for (int i = 0; i < count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + i);
            instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(INSTANCE_ID_PREFIX + i);
            instanceMetaData.setPrivateId((long) i);
            instances.add(instanceMetaData);
        }
        return instances;
    }

    private HostGroup createHostGroup(List<InstanceMetaData> instancesToDecommission) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(INSTANCE_GROUP_NAME);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        instanceGroup.setInstanceMetaData(new HashSet<>(instancesToDecommission));
        hostGroup.setInstanceGroup(instanceGroup);
        return hostGroup;
    }
}
