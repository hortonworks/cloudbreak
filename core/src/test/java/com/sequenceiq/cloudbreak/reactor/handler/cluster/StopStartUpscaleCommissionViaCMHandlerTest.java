package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
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
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartUpscaleCommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class StopStartUpscaleCommissionViaCMHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Long STACK_ID = 100L;

    private static final Long CLUSTER_ID = 101L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private StopStartUpscaleCommissionViaCMHandler underTest;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterSetupService clusterSetupService;

    @Mock
    private ClusterCommissionService clusterCommissionService;

    @BeforeEach
    void setUp() {
        setupBasicMocks();
    }

    @Test
    void testAllCommissioned() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StopStartUpscaleCommissionViaCMResult.class);

        StopStartUpscaleCommissionViaCMResult result = (StopStartUpscaleCommissionViaCMResult) selectable;
        assertThat(result.getNotRecommissionedFqdns()).hasSize(0);
        assertThat(result.getSuccessfullyCommissionedFqdns()).hasSize(commissionInstanceCount);
    }

    @Test
    void testCmHasMissingNodes() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission
                .stream().limit(commissionInstanceCount - 1).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StopStartUpscaleCommissionViaCMResult.class);

        StopStartUpscaleCommissionViaCMResult result = (StopStartUpscaleCommissionViaCMResult) selectable;
        assertThat(result.getNotRecommissionedFqdns()).hasSize(1);
        assertThat(result.getSuccessfullyCommissionedFqdns()).hasSize(commissionInstanceCount - 1);
    }

    @Test
    void testCmCommissionReturnsFewerNodes() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().limit(commissionInstanceCount - 1).collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StopStartUpscaleCommissionViaCMResult.class);

        StopStartUpscaleCommissionViaCMResult result = (StopStartUpscaleCommissionViaCMResult) selectable;
        assertThat(result.getNotRecommissionedFqdns()).hasSize(1);
        assertThat(result.getSuccessfullyCommissionedFqdns()).hasSize(commissionInstanceCount - 1);
    }

    @Test
    void testFewerNodesOnBothCmInvocations() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission
                .stream().limit(commissionInstanceCount - 1).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().limit(commissionInstanceCount - 2).collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StopStartUpscaleCommissionViaCMResult.class);

        StopStartUpscaleCommissionViaCMResult result = (StopStartUpscaleCommissionViaCMResult) selectable;
        assertThat(result.getNotRecommissionedFqdns()).hasSize(2);
        assertThat(result.getSuccessfullyCommissionedFqdns()).hasSize(commissionInstanceCount - 2);
    }

    @Test
    void testErrorFromWaitForHostsHealthy() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        doThrow(new RuntimeException("waitForHostsHealthyException")).when(clusterSetupService).waitForHostsHealthy(anySet());

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StackFailureEvent.class);

        StackFailureEvent result = (StackFailureEvent) selectable;
        assertThat(result.getException().getMessage()).isEqualTo("waitForHostsHealthyException");
    }

    @Test
    void testErrorFromCmHostCollection() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        when(clusterCommissionService.collectHostsToCommission(eq(hostGroup), eq(hostNames)))
                .thenThrow(new RuntimeException("collectHostsToCommissionError"));

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StackFailureEvent.class);

        StackFailureEvent result = (StackFailureEvent) selectable;
        assertThat(result.getException().getMessage()).isEqualTo("collectHostsToCommissionError");
    }

    @Test
    void testErrorFromCmCommission() throws ClusterClientInitException {
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstancesToCommission(commissionInstanceCount);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        when(clusterCommissionService.recommissionClusterNodes(cmAvailableHosts)).thenThrow(new RuntimeException("commissionHostsError"));

        StopStartUpscaleCommissionViaCMRequest request =
                new StopStartUpscaleCommissionViaCMRequest(stack, INSTANCE_GROUP_NAME, instancesToCommission, Collections.emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(StackFailureEvent.class);

        StackFailureEvent result = (StackFailureEvent) selectable;
        assertThat(result.getException().getMessage()).isEqualTo("commissionHostsError");
    }

    private List<InstanceMetaData> createInstancesToCommission(int count) {
        List<InstanceMetaData> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        for (int i = 0; i < count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + i);
            instanceMetaData.setInstanceStatus(InstanceStatus.STARTED);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(INSTANCE_ID_PREFIX + i);
            instances.add(instanceMetaData);
        }
        return instances;
    }

    private HostGroup createHostGroup(List<InstanceMetaData> instancesToCommission) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(INSTANCE_GROUP_NAME);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        instanceGroup.setInstanceMetaData(new HashSet<>(instancesToCommission));
        hostGroup.setInstanceGroup(instanceGroup);
        return hostGroup;
    }

    private void setupBasicMocks() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);

        lenient().when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterSetupService()).thenReturn(clusterSetupService);
        lenient().when(clusterApi.clusterCommissionService()).thenReturn(clusterCommissionService);
    }

    private void setupPerTestMocks(HostGroup hostGroup, Set<String> hostnames,
            Map<String, InstanceMetaData> cmAvailableHosts, Set<String> recommissionedFqdns) throws ClusterClientInitException {

        lenient().when(hostGroupService.getByClusterIdAndName(eq(CLUSTER_ID), eq(INSTANCE_GROUP_NAME))).thenReturn(Optional.of(hostGroup));

        lenient().doNothing().when(clusterSetupService).waitForHostsHealthy(anySet());
        lenient().when(clusterCommissionService.collectHostsToCommission(eq(hostGroup), eq(hostnames))).thenReturn(cmAvailableHosts);

        lenient().when(clusterCommissionService.recommissionClusterNodes(cmAvailableHosts)).thenReturn(recommissionedFqdns);
    }
}
