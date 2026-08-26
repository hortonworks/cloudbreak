package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_ENTERINGCMMAINTMODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleDecommissionInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleDecommissionInstancesResult;
import com.sequenceiq.cloudbreak.service.autoscale.PeriscopeClientService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class RollingVerticalScaleDecommissionInstancesHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Long STACK_ID = 100L;

    private static final String RESOURCE_CRN = "resource_crn";

    private static final Long CLUSTER_ID = 101L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String INSTANCE_ID_PREFIX_RC = "i-a-";

    private static final String FQDN_PREFIX = "fqdn-";

    private static final String FQDN_PREFIX_RC = "fqdn-a-";

    private RollingVerticalScaleResult rollingVerticalScaleResult;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private StackService stackService;

    @Mock
    private PeriscopeClientService periscopeClientService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private RollingVerticalScaleDecommissionInstancesHandler underTest;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterDecomissionService clusterDecomissionService;

    @Mock
    private ClusterHealthService clusterHealthService;

    @BeforeEach
    void setUp() {
        setupBasicMocks();
        when(stackService.getByIdWithLists(any())).thenReturn(stack);
    }

    @Test
    void testAllDecommissioned() throws Exception {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        testCollectDecommissionCombinationsInternal(5, 5, 5);
    }

    @Test
    void testAllDecommissionedWithHostgroupNotPresent() throws Exception {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of("master", Set.of("NODEMANAGER")));
        testCollectDecommissionCombinationsInternal(5, 5, 5);
    }

    @Test
    void testNoneDecommissioned() throws Exception {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("KAFKA_BROKER")));

        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, 5, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());
        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToDecommission.stream().map(InstanceMetadataView::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);
        RollingVerticalScaleDecommissionInstancesRequest request =
                new RollingVerticalScaleDecommissionInstancesRequest(STACK_ID, INSTANCE_GROUP_NAME, hostnamesToDecommission, rollingVerticalScaleResult);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(RollingVerticalScaleDecommissionInstancesResult.class);
        verifyNoInteractions(flowMessageService);
        verifyNoInteractions(clusterDecomissionService);
    }

    @Test
    void testDecommissionReturnsFewerNodes() throws Exception {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(null)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getNonGatewayComponentsByHostGroup()).thenReturn(Map.of(INSTANCE_GROUP_NAME, Set.of("NODEMANAGER")));
        testCollectDecommissionCombinationsInternal(5, 5, 3);
    }

    private void testCollectDecommissionCombinationsInternal(int instancesToDecommissionCount, int expcetedInstanceToCollectCount,
            int expectedInstancesDecommissionedCount) throws Exception {
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());
        List<String> decommissionedInstanceIds = decommissionedMetadataList.stream().map(InstanceMetadataView::getInstanceId).toList();
        Set<String> failedDecommissionNames = collected.values()
                .stream()
                .filter(instanceMetadataView -> !decommissionedInstanceIds.contains(instanceMetadataView.getInstanceId())).
                map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        rollingVerticalScaleResult = new RollingVerticalScaleResult(instancesToDecommission.stream().map(InstanceMetadataView::getInstanceId).toList(),
                INSTANCE_GROUP_NAME);
        RollingVerticalScaleDecommissionInstancesRequest request =
                new RollingVerticalScaleDecommissionInstancesRequest(STACK_ID, INSTANCE_GROUP_NAME, hostnamesToDecommission, rollingVerticalScaleResult);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(RollingVerticalScaleDecommissionInstancesResult.class);

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());

        verify(rollingVerticalScaleService).decommissionInstances(eq(STACK_ID), eq(hostnamesToDecommission), eq(INSTANCE_GROUP_NAME));
        verify(rollingVerticalScaleService).finishDecommissionInstances(eq(STACK_ID), eq(decommissionedInstanceIds), eq(INSTANCE_GROUP_NAME));
        verify(rollingVerticalScaleService).failedToDecommissionInstances(eq(STACK_ID), eq(failedDecommissionNames), eq(INSTANCE_GROUP_NAME),
                eq("Node did not report as decommissioned within the poll window"));

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_ENTERINGCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verify(clusterDecomissionService).enterMaintenanceMode(eq(fqdnsDecommissioned));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_ENTEREDCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    private void setupBasicMocks() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);
        lenient().when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);

        lenient().when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        lenient().when(clusterApi.clusterHealthService()).thenReturn(clusterHealthService);
    }

    private void setupAdditionalMocks(String hostGroupName, List<InstanceMetadataView> allInstanceMetadata,
            Map<String, InstanceMetadataView> collectedInstances, Set<String> decommissionedInstances) {
        lenient().when(stack.getNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadata);
        lenient().when(stackService.getInstanceMetadata(any(), any())).thenCallRealMethod();

        Set<String> hostnames = allInstanceMetadata.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        lenient().when(clusterDecomissionService.collectHostsToRemove(eq(hostGroupName), eq(hostnames))).thenReturn(collectedInstances);

        lenient().when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collectedInstances), anyLong())).thenReturn(decommissionedInstances);
    }

    private List<InstanceMetadataView> getInstancesWithStatus(int startIndex, int count, String instanceIdPrefix, String fqdnPrefix, InstanceStatus status) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        for (long i = startIndex; i < startIndex + count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(instanceIdPrefix + i);
            instanceMetaData.setInstanceStatus(status);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(fqdnPrefix + i);
            instanceMetaData.setPrivateId(i);
            instanceMetaData.setId(i);
            instances.add(instanceMetaData);
        }
        return instances;
    }
}