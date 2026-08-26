package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_CM_TIMEOUT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_WAITING_HOSTSTART;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleCommissionInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleCommissionInstancesResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleCommissionInstancesHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Long STACK_ID = 100L;

    private static final Long CLUSTER_ID = 11L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private RollingVerticalScaleResult rollingVerticalScaleResult;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterCommissionService clusterCommissionService;

    @Mock
    private ClusterSetupService clusterSetupService;

    @InjectMocks
    private RollingVerticalScaleCommissionInstancesHandler underTest;

    @BeforeEach
    void setUp() {
        setupBasicMocks();
        when(stackService.getByIdWithLists(any())).thenReturn(stack);
    }

    @Test
    void testAllCommissioned() throws ClusterClientInitException {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clusterSetupService.waitForHostsHealthy(new HashSet<>(instancesToCommission))).thenReturn(extendedPollingResult);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);

        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(STACK_ID,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);

        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;
        assertEquals(RollingVerticalScaleStatus.SUCCESS, result.getRollingVerticalScaleResult().getInstanceStatus().get("i-1").getStatus());

        verify(clusterCommissionService).collectHostsToCommission(eq(hostGroup), eq(hostNames));
        verify(clusterCommissionService).recommissionClusterNodes(eq(cmAvailableHosts));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_HOSTSTART), eq(String.valueOf(instancesToCommission.size())));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED), eq(String.valueOf(instancesToCommission.size())));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(INSTANCE_GROUP_NAME),
                eq(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList()));
    }

    @Test
    void testAllCommissionedWithHostGroupNotPresent() throws ClusterClientInitException {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of("master", Set.of("NODEMANAGER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clusterSetupService.waitForHostsHealthy(new HashSet<>(instancesToCommission))).thenReturn(extendedPollingResult);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);

        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(STACK_ID,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);

        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;
        assertEquals(RollingVerticalScaleStatus.SUCCESS, result.getRollingVerticalScaleResult().getInstanceStatus().get("i-1").getStatus());

        verify(clusterCommissionService).collectHostsToCommission(eq(hostGroup), eq(hostNames));
        verify(clusterCommissionService).recommissionClusterNodes(eq(cmAvailableHosts));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_HOSTSTART), eq(String.valueOf(instancesToCommission.size())));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED), eq(String.valueOf(instancesToCommission.size())));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(INSTANCE_GROUP_NAME),
                eq(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList()));
    }

    @Test
    void testNoneCommissionedWithKafka() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("KAFKA_BROKER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);

        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(1L,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);

        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;
        assertEquals(RollingVerticalScaleStatus.SUCCESS, result.getRollingVerticalScaleResult().getInstanceStatus().get("i-1").getStatus());
        verifyNoInteractions(flowMessageService);
        verifyNoInteractions(clusterCommissionService);
    }

    @Test
    void testCmCommissionReturnsFewerNodes() throws ClusterClientInitException {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clusterSetupService.waitForHostsHealthy(new HashSet<>(instancesToCommission))).thenReturn(extendedPollingResult);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().limit(commissionInstanceCount - 1).collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);

        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(STACK_ID,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);

        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;
        assertThat(result.getRollingVerticalScaleResult().getInstanceStatus().entrySet().stream()
                .filter(i -> i.getValue().getStatus().equals(RollingVerticalScaleStatus.SUCCESS))
                .collect(Collectors.toSet())).hasSize(commissionInstanceCount - 1);

        verify(clusterCommissionService).collectHostsToCommission(eq(hostGroup), eq(hostNames));
        verify(clusterCommissionService).recommissionClusterNodes(eq(cmAvailableHosts));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_HOSTSTART), eq(String.valueOf(instancesToCommission.size())));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED), eq(String.valueOf(instancesToCommission.size())));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(INSTANCE_GROUP_NAME),
                eq(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList()));
    }

    @Test
    void testFewerNodesOnBothCmInvocations() throws ClusterClientInitException {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clusterSetupService.waitForHostsHealthy(new HashSet<>(instancesToCommission))).thenReturn(extendedPollingResult);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission
                .stream().limit(commissionInstanceCount - 1).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().limit(commissionInstanceCount - 2).collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);

        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(STACK_ID,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;

        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);
        assertThat(result.getRollingVerticalScaleResult().getInstanceStatus().entrySet().stream()
                .filter(i -> i.getValue().getStatus().equals(RollingVerticalScaleStatus.SUCCESS))
                .collect(Collectors.toSet())).hasSize(commissionInstanceCount - 2);

        verify(clusterCommissionService).collectHostsToCommission(eq(hostGroup), eq(hostNames));
        verify(clusterCommissionService).recommissionClusterNodes(eq(cmAvailableHosts));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_HOSTSTART), eq(String.valueOf(instancesToCommission.size())));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED), eq(String.valueOf(instancesToCommission.size())));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(INSTANCE_GROUP_NAME),
                eq(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList()));
    }

    @Test
    void testWhenOneCmHostDidNotStart() throws ClusterClientInitException {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        ExtendedPollingResult extendedPollingResult =
                new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().withFailedInstanceIds(Set.of(5L)).build();
        when(clusterSetupService.waitForHostsHealthy(new HashSet<>(instancesToCommission))).thenReturn(extendedPollingResult);

        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);
        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(STACK_ID,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);
        instancesToCommission = createInstanceMetaDataWithStatus(4, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;

        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);
        assertThat(result.getRollingVerticalScaleResult().getInstanceStatus().entrySet().stream()
                .filter(i -> i.getValue().getStatus().equals(RollingVerticalScaleStatus.SUCCESS))
                .collect(Collectors.toSet())).hasSize(commissionInstanceCount - 1);

        verify(clusterCommissionService).collectHostsToCommission(eq(hostGroup), eq(hostNames));
        verify(clusterCommissionService).recommissionClusterNodes(eq(cmAvailableHosts));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_CM_TIMEOUT), eq(String.valueOf(instancesToCommission.size() + 1)), eq(String.valueOf(1)), eq("fqdn-4"));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_HOSTSTART), eq(String.valueOf(instancesToCommission.size() + 1)));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED), eq(String.valueOf(instancesToCommission.size())));
        verify(rollingVerticalScaleService).waitingForServicesHealthy(eq(STACK_ID), eq(INSTANCE_GROUP_NAME),
                eq(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList()));
    }

    @Test
    void testErrorFromCmHostCollection() throws ClusterClientInitException {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        int commissionInstanceCount = 5;
        List<InstanceMetaData> instancesToCommission = createInstanceMetaDataWithStatus(commissionInstanceCount, "fqdn-",
                InstanceStatus.SERVICES_RUNNING);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clusterSetupService.waitForHostsHealthy(new HashSet<>(instancesToCommission))).thenReturn(extendedPollingResult);
        HostGroup hostGroup = createHostGroup(instancesToCommission);

        Set<String> hostNames = instancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
        Map<String, InstanceMetaData> cmAvailableHosts = instancesToCommission.stream().collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        Set<String> recommissionedFqdns = cmAvailableHosts.keySet().stream().collect(Collectors.toUnmodifiableSet());

        setupPerTestMocks(hostGroup, hostNames, cmAvailableHosts,  recommissionedFqdns);
        when(clusterCommissionService.collectHostsToCommission(eq(hostGroup), eq(hostNames)))
                .thenThrow(new RuntimeException("collectHostsToCommissionError"));

        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);
        RollingVerticalScaleCommissionInstancesRequest request =
                new RollingVerticalScaleCommissionInstancesRequest(STACK_ID,  INSTANCE_GROUP_NAME, new ArrayList<>(instancesToCommission),
                        rollingVerticalScaleResult);

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        assertThat(selectable).isInstanceOf(RollingVerticalScaleCommissionInstancesResult.class);

        RollingVerticalScaleCommissionInstancesResult result = (RollingVerticalScaleCommissionInstancesResult) selectable;
        assertThat(result.getRollingVerticalScaleResult().getInstanceStatus().entrySet().stream()
                .filter(i -> i.getValue().getStatus().equals(RollingVerticalScaleStatus.SUCCESS))
                .collect(Collectors.toSet())).hasSize(0);

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_HOSTSTART), eq(String.valueOf(instancesToCommission.size())));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED), eq(String.valueOf(instancesToCommission.size())));
        verify(rollingVerticalScaleService).failedCommissionInstances(eq(STACK_ID),
                eq(instancesToCommission.stream().map(InstanceMetaData::getInstanceId).toList()), eq(INSTANCE_GROUP_NAME),
                eq("collectHostsToCommissionError"));
    }

    private List<InstanceMetaData> createInstanceMetaDataWithStatus(int count, String fqdnPrefix, InstanceStatus status) {
        List<InstanceMetaData> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        for (int i = 0; i < count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setPrivateId(i + 1L);
            instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + i);
            instanceMetaData.setInstanceStatus(status);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(fqdnPrefix + i);
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

        lenient().when(clusterCommissionService.collectHostsToCommission(eq(hostGroup), eq(hostnames))).thenReturn(cmAvailableHosts);

        lenient().when(clusterCommissionService.recommissionClusterNodes(cmAvailableHosts)).thenReturn(recommissionedFqdns);
    }
}